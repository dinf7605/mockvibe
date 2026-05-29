package com.fintech.simulator.market.provider.kis;

import com.fintech.simulator.market.domain.PriceHistory;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.repository.PriceHistoryRepository;
import com.fintech.simulator.market.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KIS 일봉 실데이터 수집기.
 *
 * <h3>스케줄</h3>
 * <ol>
 *   <li><b>부팅 시 1회</b> ({@link ApplicationRunner}, 비동기) — 최근 1년치 backfill.
 *       이미 시드(랜덤워크)된 행은 update() 로 덮어쓴다. order=20 으로 V4 시드러보다 늦게.</li>
 *   <li><b>매일 17:00 KST</b> ({@link Scheduled}) — KRX 장 마감 후. 최근 7일치 재fetch + UPSERT.</li>
 * </ol>
 *
 * <h3>속도 조절</h3>
 * KisRestClient 내부에서 Bucket4j ({@code KisRateLimiter}) 로 자동 throttling.
 * 30종목 × 3페이지(100건씩) = 90 호출. KIS 모의투자 한도(분당 ~60건) 안에서 약 1~2분 소요.
 *
 * <h3>회복력</h3>
 * 페이지 단위로 호출 — 한 종목 또는 한 페이지 실패해도 다음 종목 계속 진행.
 * Resilience4j CB(kis-rest) 가 폭주 차단.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(20)   // PriceHistorySeeder(default order) 이후에 실행
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisDailyCandleFetcher implements ApplicationRunner {

    private static final DateTimeFormatter API_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** KIS 한 페이지 최대 거래일 수 */
    private static final int PAGE_DAYS = 100;
    /** 부팅 backfill 범위 */
    private static final int BACKFILL_DAYS = 365;
    /** cron 갱신 범위 */
    private static final int CRON_REFRESH_DAYS = 7;

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final KisDailyCandleClient client;

    @Override
    @Async
    public void run(ApplicationArguments args) {
        log.info("KisDailyCandleFetcher: boot backfill 시작 ({} days)", BACKFILL_DAYS);
        int total = fetchAll(BACKFILL_DAYS);
        log.info("KisDailyCandleFetcher: boot backfill 완료 — 총 {} 일봉 행 UPSERT", total);
    }

    /**
     * 매일 KST 17:00 (KRX 장 마감 ~15:30 + 정산 마진) 에 최근 7일치 재fetch.
     * 중복은 update() 로 덮어쓴다 (idempotent).
     */
    @Scheduled(cron = "0 0 17 * * MON-FRI", zone = "Asia/Seoul")
    @Async
    public void dailyRefresh() {
        log.info("KisDailyCandleFetcher: 일일 cron 시작 ({} days)", CRON_REFRESH_DAYS);
        int total = fetchAll(CRON_REFRESH_DAYS);
        log.info("KisDailyCandleFetcher: 일일 cron 완료 — {} 일봉 행 UPSERT", total);
    }

    private int fetchAll(int days) {
        List<Stock> krxStocks = stockRepository.findAll().stream()
                .filter(s -> "KRX".equals(s.getMarket()))
                .toList();

        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusDays(days);

        int totalSaved = 0;
        for (Stock s : krxStocks) {
            try {
                int saved = fetchForTicker(s, from, today);
                if (saved > 0) syncCurrentPrice(s);
                totalSaved += saved;
            } catch (Exception e) {
                log.warn("KIS daily fetch failed for {}: {}", s.getTicker(), e.toString());
                // 한 종목 실패해도 계속 진행
            }
        }
        return totalSaved;
    }

    /**
     * STOCKS.current_price 를 가장 최근 PRICE_HISTORY close 로 동기화.
     *
     * <p>{@code Stock.current_price} 는 검색·상세·포트폴리오 표시(캐시 miss 시)에 직접 노출되는
     * denormalized 컬럼이다. PRICE_HISTORY 만 UPSERT 하면 차트만 실데이터로 바뀌고
     * "현재가" 는 PriceHistorySeeder 가 부팅 1회 박아둔 mock seed 의 마지막 close 에 고정된다.
     */
    private void syncCurrentPrice(Stock stock) {
        priceHistoryRepository.findTopByTickerOrderByTradeDateDesc(stock.getTicker())
                .ifPresent(latest -> {
                    stock.setCurrentPrice(latest.getClose());
                    stockRepository.save(stock);
                });
    }

    /**
     * 한 종목의 [from, to] 구간을 페이지 단위로 슬라이딩하며 fetch + UPSERT.
     * KIS 응답이 최신→과거 순이라 to 를 줄여가며 100건씩.
     */
    private int fetchForTicker(Stock stock, LocalDate from, LocalDate to) {
        int saved = 0;
        LocalDate pageTo = to;
        while (!pageTo.isBefore(from)) {
            LocalDate pageFrom = pageTo.minusDays(PAGE_DAYS - 1);
            if (pageFrom.isBefore(from)) pageFrom = from;

            List<KisDailyChartResponse.KisDailyCandleItem> items =
                    client.fetch(stock.getTicker(), pageFrom, pageTo);
            if (items.isEmpty()) {
                log.debug("Empty page for {} [{}~{}]", stock.getTicker(), pageFrom, pageTo);
                break;
            }

            saved += upsertItems(stock.getTicker(), items);

            // 다음 페이지: 이번 페이지의 가장 오래된 일자 - 1
            // KIS 응답은 최신 → 과거 순. 마지막(가장 오래된) 일자가 끝.
            LocalDate oldestInPage = LocalDate.parse(
                    items.get(items.size() - 1).tradeDate(), API_DATE_FMT);
            pageTo = oldestInPage.minusDays(1);
        }
        return saved;
    }

    /**
     * UNIQUE (ticker, trade_date) 충돌 시 update, 아니면 insert.
     * 양쪽 분기 모두 save() 명시 — 이 메서드는 self-invocation(run→fetchAll→fetchForTicker)
     * 으로 호출돼 트랜잭션 프록시가 우회되므로 ambient 트랜잭션이 없다. update 분기가
     * detached 엔티티의 dirty checking 에만 의존하면 변경이 유실(no-op)된다.
     */
    public int upsertItems(String ticker, List<KisDailyChartResponse.KisDailyCandleItem> items) {
        int count = 0;
        for (KisDailyChartResponse.KisDailyCandleItem item : items) {
            try {
                LocalDate date = LocalDate.parse(item.tradeDate(), API_DATE_FMT);
                BigDecimal o = new BigDecimal(item.openPrice());
                BigDecimal h = new BigDecimal(item.highPrice());
                BigDecimal l = new BigDecimal(item.lowPrice());
                BigDecimal c = new BigDecimal(item.closePrice());
                long vol = Long.parseLong(item.volume());

                priceHistoryRepository.findByTickerAndTradeDate(ticker, date)
                        .ifPresentOrElse(
                                existing -> {
                                    existing.update(o, h, l, c, vol);
                                    priceHistoryRepository.save(existing);
                                },
                                () -> priceHistoryRepository.save(
                                        PriceHistory.of(ticker, date, o, h, l, c, vol))
                        );
                count++;
            } catch (Exception e) {
                log.warn("Bad daily candle item for {}: {} — skip", ticker, item, e);
            }
        }
        return count;
    }
}
