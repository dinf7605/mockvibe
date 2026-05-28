package com.fintech.simulator.market.provider.yahoo;

import com.fintech.simulator.market.domain.PriceHistory;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.dto.DailyCandle;
import com.fintech.simulator.market.repository.PriceHistoryRepository;
import com.fintech.simulator.market.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 미국 종목 일봉 실데이터 수집기 (Yahoo Finance). KIS 일봉 수집기(KRX)의 미국판.
 *
 * <h3>왜 필요한가</h3>
 * 기존에 미국 종목 일봉은 {@code PriceHistorySeeder} 의 랜덤워크 mock 만 있었다
 * (Finnhub 무료가 일봉 미지원). 차트·리스크·백테스트가 가짜 데이터 위에서 돈다.
 * Yahoo {@code /v8/finance/chart/} 로 실제 OHLCV 를 받아 덮어쓴다.
 *
 * <h3>스케줄</h3>
 * <ol>
 *   <li><b>부팅 시 1회</b> (비동기) — 최근 1년치 backfill. seeder mock 을 실데이터로 UPSERT.</li>
 *   <li><b>매일 06:00 KST</b> (TUE~SAT) — 미국 장 마감(16:00 ET ≈ 05~06 KST 익일) 후 최근 7일 재fetch.</li>
 * </ol>
 *
 * <h3>회복력</h3>
 * 종목 단위 try/catch — 한 종목 실패해도 다음 종목 계속. 키 불필요라 항상 활성.
 * UPSERT 는 양쪽 분기 모두 명시적 save() (self-invocation 으로 ambient tx 없음).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(21)   // KIS 일봉 fetcher(@Order 20) 다음, seeder(default) 보다 먼저 비동기 시작
public class UsDailyCandleFetcher implements ApplicationRunner {

    private static final int BACKFILL_DAYS = 365;
    private static final int CRON_REFRESH_DAYS = 7;

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final YahooDailyCandleClient client;

    @Override
    @Async
    public void run(ApplicationArguments args) {
        log.info("UsDailyCandleFetcher: boot backfill 시작 ({} days)", BACKFILL_DAYS);
        int total = fetchAll(BACKFILL_DAYS);
        log.info("UsDailyCandleFetcher: boot backfill 완료 — 총 {} 일봉 행 UPSERT", total);
    }

    @Scheduled(cron = "0 0 6 * * TUE-SAT", zone = "Asia/Seoul")
    @Async
    public void dailyRefresh() {
        log.info("UsDailyCandleFetcher: 일일 cron 시작 ({} days)", CRON_REFRESH_DAYS);
        int total = fetchAll(CRON_REFRESH_DAYS);
        log.info("UsDailyCandleFetcher: 일일 cron 완료 — {} 일봉 행 UPSERT", total);
    }

    private int fetchAll(int days) {
        List<Stock> usStocks = stockRepository.findAll().stream()
                .filter(s -> "USD".equals(s.getCurrency()))
                .toList();

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);

        int total = 0;
        for (Stock s : usStocks) {
            try {
                List<DailyCandle> candles = client.fetch(s.getTicker(), from, to);
                total += upsert(s.getTicker(), candles);
            } catch (Exception e) {
                log.warn("US daily fetch failed for {}: {}", s.getTicker(), e.toString());
                // 한 종목 실패해도 계속 진행
            }
        }
        return total;
    }

    /**
     * UNIQUE (ticker, trade_date) 충돌 시 update, 아니면 insert.
     * 양쪽 분기 모두 save() 명시 — self-invocation 이라 ambient 트랜잭션이 없어
     * detached entity 의 dirty checking 에 의존할 수 없다.
     */
    private int upsert(String ticker, List<DailyCandle> candles) {
        int count = 0;
        for (DailyCandle c : candles) {
            long vol = c.volume() == null ? 0L : c.volume();
            priceHistoryRepository.findByTickerAndTradeDate(ticker, c.time())
                    .ifPresentOrElse(
                            existing -> {
                                existing.update(c.open(), c.high(), c.low(), c.close(), vol);
                                priceHistoryRepository.save(existing);
                            },
                            () -> priceHistoryRepository.save(
                                    PriceHistory.of(ticker, c.time(),
                                            c.open(), c.high(), c.low(), c.close(), vol))
                    );
            count++;
        }
        return count;
    }
}
