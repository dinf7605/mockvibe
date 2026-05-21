package com.fintech.simulator.market.websocket;

import com.fintech.simulator.market.controller.PriceResponse;
import com.fintech.simulator.market.event.PriceUpdatedEvent;
import com.fintech.simulator.market.provider.Quote;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PriceBroadcasterTest {

    @Mock SimpMessagingTemplate template;
    MeterRegistry registry;
    PriceBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        broadcaster = new PriceBroadcaster(template, registry);
        broadcaster.init();
    }

    @Test
    @DisplayName("PriceUpdatedEvent → /topic/price/{ticker} 로 PriceResponse 전송, lag 메트릭 기록")
    void broadcasts_and_records_metrics() {
        Quote q = new Quote("AAPL", new BigDecimal("110"), new BigDecimal("100"), Instant.now());
        broadcaster.on(new PriceUpdatedEvent(q));

        ArgumentCaptor<String> dest = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PriceResponse> payload = ArgumentCaptor.forClass(PriceResponse.class);
        verify(template).convertAndSend(dest.capture(), payload.capture());

        assertThat(dest.getValue()).isEqualTo("/topic/price/AAPL");
        assertThat(payload.getValue().changePct()).isEqualByComparingTo("10.00");
        assertThat(registry.get("market.broadcast.count").counter().count()).isEqualTo(1);
        assertThat(registry.get("market.broadcast.late.count").counter().count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Quote.timestamp가 1초 과거면 late 카운터 증가 (SLA 100ms 초과)")
    void late_broadcast_increments_late_counter() {
        Quote q = new Quote("NVDA", new BigDecimal("150"), null, Instant.now().minusSeconds(1));
        broadcaster.on(new PriceUpdatedEvent(q));

        assertThat(registry.get("market.broadcast.late.count").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("부하: 100 이벤트 처리 — 모두 SLA 이내, late=0, 전체 처리시간 1초 미만")
    void load_100_events_within_sla() {
        long startMs = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            Quote q = new Quote("T" + i, new BigDecimal("100"), null, Instant.now());
            broadcaster.on(new PriceUpdatedEvent(q));
        }
        long elapsed = System.currentTimeMillis() - startMs;

        verify(template, times(100)).convertAndSend(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Object.class));
        assertThat(registry.get("market.broadcast.count").counter().count()).isEqualTo(100);
        assertThat(registry.get("market.broadcast.late.count").counter().count()).isEqualTo(0);
        assertThat(elapsed).isLessThan(1_000); // 100건 전체 1초 미만 (in-process)
    }
}
