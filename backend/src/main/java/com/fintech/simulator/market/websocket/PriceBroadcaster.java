package com.fintech.simulator.market.websocket;

import com.fintech.simulator.market.controller.PriceResponse;
import com.fintech.simulator.market.event.PriceUpdatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * PriceUpdatedEvent → STOMP 브로드캐스트.
 * - 토픽: /topic/price/{ticker}
 * - 비동기 처리(@Async): 시세 수신 스레드를 막지 않음
 * - 목표 지연: 외부 수신 후 100ms 이내 (PRD NFR-1)
 *
 * 노출 메트릭 (Prometheus /actuator/prometheus):
 *   - market_broadcast_lag_seconds (Timer): Quote.timestamp → 브로드캐스트 송신 시점 지연
 *   - market_broadcast_count_total (Counter): 누적 브로드캐스트 횟수
 *   - market_broadcast_late_count_total: 100ms 초과 케이스 카운트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceBroadcaster {

    private static final String TOPIC_PREFIX = "/topic/price/";
    private static final long SLA_LAG_MS = 100L;

    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    private Timer lagTimer;
    private Counter broadcastCounter;
    private Counter lateCounter;

    @PostConstruct
    void init() {
        this.lagTimer = Timer.builder("market.broadcast.lag")
                .description("외부 시세 수신(Quote.timestamp) → STOMP 송신 시점 지연")
                .register(meterRegistry);
        this.broadcastCounter = Counter.builder("market.broadcast.count")
                .description("브로드캐스트 누적 횟수")
                .register(meterRegistry);
        this.lateCounter = Counter.builder("market.broadcast.late.count")
                .description("SLA(100ms) 초과 브로드캐스트 횟수")
                .register(meterRegistry);
    }

    @Async
    @EventListener
    public void on(PriceUpdatedEvent event) {
        String dest = TOPIC_PREFIX + event.quote().ticker();
        messagingTemplate.convertAndSend(dest, PriceResponse.from(event.quote()));

        long lagMs = Math.max(0, Instant.now().toEpochMilli() - event.quote().timestamp().toEpochMilli());
        lagTimer.record(lagMs, TimeUnit.MILLISECONDS);
        broadcastCounter.increment();
        if (lagMs > SLA_LAG_MS) {
            lateCounter.increment();
            log.warn("Late broadcast: ticker={}, lag={}ms (SLA={}ms)", event.quote().ticker(), lagMs, SLA_LAG_MS);
        } else if (log.isTraceEnabled()) {
            log.trace("Broadcast → {} price={} lag={}ms", dest, event.quote().price(), lagMs);
        }
    }
}
