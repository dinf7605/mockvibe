package com.fintech.simulator.market.service;

import com.fintech.simulator.market.cache.PriceCache;
import com.fintech.simulator.market.domain.IntradayCandle;
import com.fintech.simulator.market.domain.PriceHistory;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.event.PriceUpdatedEvent;
import com.fintech.simulator.market.provider.MarketDataProvider;
import com.fintech.simulator.market.provider.Quote;
import com.fintech.simulator.market.repository.IntradayCandleRepository;
import com.fintech.simulator.market.repository.PriceHistoryRepository;
import com.fintech.simulator.market.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 백그라운드 시세 폴링.
 *
 * <h3>목적</h3>
 * KIS/Finnhub WebSocket 이 불안정해도 (D49 운영에서 KIS WS connect 실패 관측됨)
 * 사용자 접속 여부와 무관하게 장 시간 중 시세를 채운다.
 *
 * <h3>동작</h3>
 * <ul>
 *   <li>60초마다 ({@code fixedDelay}) 깨어남</li>
 *   <li>{@link MarketHoursService} 로 열린 시장만 폴링 (불필요 호출 방지)</li>
 *   <li>각 종목 quote → (1) PriceCache.put (2) PriceUpdatedEvent 발행 → STOMP 브로드캐스트
 *       (3) 오늘 PRICE_HISTORY intraday UPSERT</li>
 *   <li>Bucket4j (KIS/Finnhub RateLimiter) 가 분당 한도 자동 throttle</li>
 *   <li>한 종목 실패해도 다음 종목 계속</li>
 * </ul>
 *
 * <h3>Rate budget</h3>
 * 한 시장 30종목 / 60초 = 분당 30 호출. KIS·Finnhub 무료 한도(분당 60) 안전.
 * 두 시장이 동시에 열리는 구간은 거의 없음 (KRX 주간 / US 야간).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketPollingScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ZoneId ET  = ZoneId.of("America/New_York");

    /** 분봉 보관일 (이후 일일 purge) */
    private static final int INTRADAY_RETENTION_DAYS = 3;

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final IntradayCandleRepository intradayCandleRepository;
    private final PriceCache priceCache;
    private final MarketHoursService marketHours;
    private final List<MarketDataProvider> providers;   // 활성화된 provider 만 (ConditionalOnProperty)
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 60_000L, initialDelay = 20_000L)
    public void poll() {
        boolean krx = marketHours.isKrxOpen();
        boolean us  = marketHours.isUsOpen();
        if (!krx && !us) return;   // 둘 다 닫힘 → 외부 호출 안 함

        List<Stock> stocks = stockRepository.findAll();
        int updated = 0;
        for (Stock s : stocks) {
            boolean krxStock = "KRX".equals(s.getMarket());
            if (krxStock && !krx) continue;
            if (!krxStock && !us) continue;

            try {
                if (pollOne(s)) updated++;
            } catch (Exception e) {
                log.debug("poll failed for {}: {}", s.getTicker(), e.toString());
            }
        }
        if (updated > 0) log.debug("MarketPolling: {} tickers updated (krx={}, us={})", updated, krx, us);
    }

    private boolean pollOne(Stock stock) {
        MarketDataProvider provider = providers.stream()
                .filter(p -> p.supports(stock.getTicker()))
                .findFirst().orElse(null);
        if (provider == null) return false;

        return provider.getQuote(stock.getTicker())
                .map(q -> {
                    priceCache.put(q);
                    eventPublisher.publishEvent(new PriceUpdatedEvent(q));  // → STOMP
                    recordIntraday(q, tradeDateOf(stock));
                    recordMinute(q);
                    syncStockPrice(stock, q.price());
                    return true;
                })
                .orElse(false);
    }

    /**
     * 폴링 시세로 STOCKS.current_price 갱신.
     *
     * <p>이 컬럼은 종목 검색·상세의 "현재가"(StockResponse)와 포트폴리오 캐시 miss 폴백에
     * 직접 노출되는 denormalized 값이다. 갱신하지 않으면 장중에 시세가 움직여도 일봉 backfill
     * 동기화(하루 1회)·부팅 시점 값에 멈춰 있다. 매 폴링마다 최신 native 가격으로 sync.
     * self-invocation 이라 명시 save().
     */
    private void syncStockPrice(Stock stock, java.math.BigDecimal nativePrice) {
        if (nativePrice == null || nativePrice.signum() <= 0) return;
        stock.setCurrentPrice(nativePrice);
        stockRepository.save(stock);
    }

    /**
     * 분봉(INTRADAY_CANDLE) 누적. 폴링이 분당 1회라 대개 한 분에 1틱(O=H=L=C),
     * 같은 분 재호출 시 high/low/close 갱신. self-invocation → 명시 save().
     */
    private void recordMinute(Quote q) {
        OffsetDateTime bucket = OffsetDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        intradayCandleRepository.findByTickerAndBucketTs(q.ticker(), bucket)
                .ifPresentOrElse(
                        existing -> {
                            existing.applyTick(q.price());
                            intradayCandleRepository.save(existing);
                        },
                        () -> intradayCandleRepository.save(
                                IntradayCandle.openOf(q.ticker(), bucket, q.price()))
                );
    }

    /** 분봉 보관 정책 — 매일 새벽 오래된 분봉 삭제(테이블 비대 방지). */
    @Scheduled(cron = "0 30 5 * * *", zone = "Asia/Seoul")
    @Transactional
    public void purgeOldIntraday() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(INTRADAY_RETENTION_DAYS);
        int deleted = intradayCandleRepository.deleteOlderThan(cutoff);
        if (deleted > 0) log.info("Intraday purge: {} candles older than {} days deleted", deleted, INTRADAY_RETENTION_DAYS);
    }

    /**
     * 종목 시장의 "거래일" 을 그 시장 로컬 타임존 기준으로 결정한다.
     *
     * <p>JVM 기본 타임존이 KST 라 {@code LocalDate.now()} 는 항상 KST 날짜를 준다.
     * 그런데 미국 장(09:30~16:00 ET = 22:30~05:00 KST)은 하나의 ET 세션이 KST 로는
     * 두 날짜에 걸친다. KST 날짜로 기록하면 세션 후반(02:00~05:00 KST)의 intraday 가
     * 다음 날 KST 날짜로 저장돼, ET 날짜를 쓰는 Yahoo 일봉과 어긋나며 유령 캔들이 생긴다.
     * → 시장별로 올바른 타임존의 LocalDate 를 쓴다.
     */
    private LocalDate tradeDateOf(Stock stock) {
        return "KRX".equals(stock.getMarket())
                ? LocalDate.now(KST)
                : LocalDate.now(ET);
    }

    /**
     * 거래일 PRICE_HISTORY candle 에 현재가 누적 (high/low/close).
     * self-invocation 이라 @Transactional 대신 save() 를 명시해 merge 저장.
     */
    private void recordIntraday(Quote q, LocalDate tradeDate) {
        priceHistoryRepository.findByTickerAndTradeDate(q.ticker(), tradeDate)
                .ifPresentOrElse(
                        existing -> {
                            existing.applyIntraday(q.price());
                            priceHistoryRepository.save(existing);
                        },
                        () -> priceHistoryRepository.save(
                                PriceHistory.intradayOpen(q.ticker(), tradeDate, q.price()))
                );
    }
}
