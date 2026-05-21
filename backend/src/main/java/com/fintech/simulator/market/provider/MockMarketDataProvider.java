package com.fintech.simulator.market.provider;

import com.fintech.simulator.market.cache.PriceCache;
import com.fintech.simulator.market.event.PriceUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 랜덤워크 기반 Mock 시세 공급자.
 *
 * - 외부 API 장애·미발급 환경에서도 데모 가능 (PRD §9 Fallback)
 * - 2초 간격으로 가격을 ±0.5% 변동시켜 PriceCache 갱신 + PriceUpdatedEvent 발행 (D11)
 * - app.market.mock.enabled=false 면 비활성
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.market.mock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockMarketDataProvider implements MarketDataProvider {

    private static final BigDecimal MAX_DELTA_PCT = new BigDecimal("0.005"); // ±0.5%

    /** 시드 종목 — D08의 STOCKS 마스터가 들어오기 전까지의 임시 더미 */
    private static final Map<String, BigDecimal> SEED_PRICES = Map.of(
            "005930", new BigDecimal("78000"),     // 삼성전자 (KRW)
            "AAPL",   new BigDecimal("225.00"),    // Apple (USD)
            "NVDA",   new BigDecimal("145.00")     // NVIDIA (USD)
    );

    private final PriceCache priceCache;
    private final ApplicationEventPublisher eventPublisher;
    private final ConcurrentHashMap<String, BigDecimal> currentPrices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BigDecimal> prevCloses = new ConcurrentHashMap<>();

    @Value("${app.market.mock.tick-interval-ms:2000}")
    private long tickIntervalMs;

    public MockMarketDataProvider(PriceCache priceCache, ApplicationEventPublisher eventPublisher) {
        this.priceCache = priceCache;
        this.eventPublisher = eventPublisher;
        SEED_PRICES.forEach((t, p) -> {
            currentPrices.put(t, p);
            prevCloses.put(t, p);
            priceCache.put(toQuote(t, p, p));
        });
    }

    @Override
    public String name() {
        return "MOCK";
    }

    @Override
    public boolean supports(String ticker) {
        return currentPrices.containsKey(ticker);
    }

    @Override
    public Optional<Quote> getQuote(String ticker) {
        BigDecimal current = currentPrices.get(ticker);
        if (current == null) return Optional.empty();
        return Optional.of(toQuote(ticker, current, prevCloses.get(ticker)));
    }

    /** 일정 주기로 가격 변동 시뮬레이션 */
    @Scheduled(fixedDelayString = "${app.market.mock.tick-interval-ms:2000}")
    public void tick() {
        currentPrices.forEach((ticker, prev) -> {
            BigDecimal next = nextPrice(prev);
            currentPrices.put(ticker, next);
            Quote q = toQuote(ticker, next, prevCloses.get(ticker));
            priceCache.put(q);
            eventPublisher.publishEvent(new PriceUpdatedEvent(q));
        });
        log.trace("Mock tick: {} tickers updated (interval={}ms)", currentPrices.size(), tickIntervalMs);
    }

    private BigDecimal nextPrice(BigDecimal prev) {
        // [-MAX_DELTA_PCT, +MAX_DELTA_PCT) 범위의 변동률
        double delta = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * MAX_DELTA_PCT.doubleValue();
        BigDecimal multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(delta));
        return prev.multiply(multiplier).setScale(prev.scale(), RoundingMode.HALF_UP);
    }

    private Quote toQuote(String ticker, BigDecimal price, BigDecimal prevClose) {
        return new Quote(ticker, price, prevClose, Instant.now());
    }
}
