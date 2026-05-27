package com.fintech.simulator.market.service;

import com.fintech.simulator.market.cache.PriceCache;
import com.fintech.simulator.market.domain.PriceHistory;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.event.PriceUpdatedEvent;
import com.fintech.simulator.market.provider.MarketDataProvider;
import com.fintech.simulator.market.provider.Quote;
import com.fintech.simulator.market.repository.PriceHistoryRepository;
import com.fintech.simulator.market.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;
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
                    recordIntraday(q);
                    return true;
                })
                .orElse(false);
    }

    /**
     * 오늘 PRICE_HISTORY candle 에 현재가 누적 (high/low/close).
     * self-invocation 이라 @Transactional 대신 save() 를 명시해 merge 저장.
     */
    private void recordIntraday(Quote q) {
        LocalDate today = LocalDate.now();
        priceHistoryRepository.findByTickerAndTradeDate(q.ticker(), today)
                .ifPresentOrElse(
                        existing -> {
                            existing.applyIntraday(q.price());
                            priceHistoryRepository.save(existing);
                        },
                        () -> priceHistoryRepository.save(
                                PriceHistory.intradayOpen(q.ticker(), today, q.price()))
                );
    }
}
