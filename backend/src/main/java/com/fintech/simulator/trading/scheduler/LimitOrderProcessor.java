package com.fintech.simulator.trading.scheduler;

import com.fintech.simulator.market.event.PriceUpdatedEvent;
import com.fintech.simulator.trading.domain.LimitOrder;
import com.fintech.simulator.trading.domain.LimitOrderStatus;
import com.fintech.simulator.trading.repository.LimitOrderRepository;
import com.fintech.simulator.trading.service.LimitOrderFiller;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 시세 갱신 이벤트 기반 지정가 체결 처리기 (PRD §7.2 ADR-3).
 *
 *  - 입력: PriceUpdatedEvent (PriceCache 갱신 시점 발행)
 *  - 처리: 해당 ticker의 PENDING 주문 조회 → matches() 평가 → LimitOrderFiller에 위임
 *  - 인덱스: idx_lo_ticker_status_price (D21 V3)
 *  - 동일 ticker 동시 처리 방지: per-ticker 락 (체결 후보 중복 처리 차단)
 *  - 만료 처리는 D24 별도 배치
 *
 * D22 골격 — 실제 체결 트랜잭션은 D23 LimitOrderFiller에서 정밀화.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LimitOrderProcessor {

    private final LimitOrderRepository limitOrderRepository;
    private final LimitOrderFiller filler;
    private final MeterRegistry meterRegistry;

    private final ConcurrentMap<String, Object> tickerLocks = new ConcurrentHashMap<>();

    private Timer processTimer;
    private Counter candidateCounter;
    private Counter filledCounter;
    private Counter expiredOnSightCounter;

    @PostConstruct
    void init() {
        this.processTimer = Timer.builder("limit_order.process.lag")
                .description("PriceUpdatedEvent → 후보 처리 완료까지 지연")
                .register(meterRegistry);
        this.candidateCounter = Counter.builder("limit_order.candidate.count")
                .description("matches() true인 후보 누적 수")
                .register(meterRegistry);
        this.filledCounter = Counter.builder("limit_order.filled.count")
                .description("체결 성공 누적 수")
                .register(meterRegistry);
        this.expiredOnSightCounter = Counter.builder("limit_order.expired_on_sight.count")
                .description("후보 조회 시점에 이미 expires_at 지난 주문 카운트")
                .register(meterRegistry);
    }

    @Async
    @EventListener
    public void on(PriceUpdatedEvent event) {
        long startNs = System.nanoTime();
        try {
            processForTicker(event.quote().ticker(), event.quote().price());
        } finally {
            processTimer.record(System.nanoTime() - startNs, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 한 ticker에 대한 후보 평가.
     * - per-ticker 락: 동일 종목 시세가 빠르게 연속 갱신될 때 중복 후보 처리 차단
     * - 트랜잭션은 LimitOrderFiller 안에서 시작 (체결당 단일 트랜잭션)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processForTicker(String ticker, BigDecimal currentPrice) {
        Object lock = tickerLocks.computeIfAbsent(ticker, k -> new Object());
        synchronized (lock) {
            List<LimitOrder> candidates =
                    limitOrderRepository.findByTickerAndStatus(ticker, LimitOrderStatus.PENDING);
            if (candidates.isEmpty()) return;

            OffsetDateTime now = OffsetDateTime.now();
            for (LimitOrder lo : candidates) {
                if (lo.getExpiresAt() != null && lo.getExpiresAt().isBefore(now)) {
                    expiredOnSightCounter.increment();
                    continue;   // 만료 처리는 D24 배치에서. 여기서는 skip만.
                }
                if (!lo.matches(currentPrice)) continue;

                candidateCounter.increment();
                try {
                    filler.fill(lo.getLimitOrderId(), currentPrice);
                    filledCounter.increment();
                } catch (Exception e) {
                    // 잔고 부족·보유 부족 등으로 한 주문이 실패해도 다른 후보는 계속 처리
                    log.warn("Limit fill skipped: id={} ticker={} reason={}",
                            lo.getLimitOrderId(), ticker, e.getMessage());
                }
            }
        }
    }
}
