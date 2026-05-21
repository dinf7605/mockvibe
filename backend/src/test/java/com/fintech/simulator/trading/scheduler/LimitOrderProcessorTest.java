package com.fintech.simulator.trading.scheduler;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.event.PriceUpdatedEvent;
import com.fintech.simulator.market.provider.Quote;
import com.fintech.simulator.trading.domain.LimitOrder;
import com.fintech.simulator.trading.domain.LimitOrderStatus;
import com.fintech.simulator.trading.domain.OrderSide;
import com.fintech.simulator.trading.repository.LimitOrderRepository;
import com.fintech.simulator.trading.service.LimitOrderFiller;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LimitOrderProcessorTest {

    @Mock LimitOrderRepository limitOrderRepository;
    @Mock LimitOrderFiller filler;
    MeterRegistry registry;
    LimitOrderProcessor processor;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        processor = new LimitOrderProcessor(limitOrderRepository, filler, registry);
        processor.init();
    }

    private LimitOrder pending(OrderSide side, String price, OffsetDateTime expiresAt) {
        return LimitOrder.register("u1", "AAPL", side, new BigDecimal(price), BigDecimal.ONE, expiresAt);
    }

    private void fire(BigDecimal price) {
        processor.on(new PriceUpdatedEvent(new Quote("AAPL", price, null, Instant.now())));
    }

    @Test
    @DisplayName("PENDING 후보 없으면 filler 호출 없음")
    void no_candidates() {
        given(limitOrderRepository.findByTickerAndStatus("AAPL", LimitOrderStatus.PENDING))
                .willReturn(List.of());

        fire(new BigDecimal("100"));

        verify(filler, never()).fill(anyLong(), any());
    }

    @Test
    @DisplayName("BUY 후보: 현재가 <= target 인 주문만 fill 호출")
    void buy_match_only() {
        LimitOrder match = pending(OrderSide.BUY, "100", OffsetDateTime.now().plusDays(1));
        LimitOrder nomatch = pending(OrderSide.BUY, "90", OffsetDateTime.now().plusDays(1));
        given(limitOrderRepository.findByTickerAndStatus("AAPL", LimitOrderStatus.PENDING))
                .willReturn(List.of(match, nomatch));

        fire(new BigDecimal("95"));

        verify(filler, times(1)).fill(any(), eq(new BigDecimal("95")));
        assertThat(registry.get("limit_order.candidate.count").counter().count()).isEqualTo(1);
        assertThat(registry.get("limit_order.filled.count").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("SELL 후보: 현재가 >= target 인 주문만 fill 호출")
    void sell_match_only() {
        LimitOrder match = pending(OrderSide.SELL, "100", OffsetDateTime.now().plusDays(1));
        given(limitOrderRepository.findByTickerAndStatus("AAPL", LimitOrderStatus.PENDING))
                .willReturn(List.of(match));

        fire(new BigDecimal("105"));

        verify(filler, times(1)).fill(any(), any());
    }

    @Test
    @DisplayName("만료된 PENDING은 즉시 skip (expired_on_sight 카운터)")
    void skip_expired() {
        LimitOrder expired = pending(OrderSide.BUY, "200", OffsetDateTime.now().minusDays(1));
        given(limitOrderRepository.findByTickerAndStatus("AAPL", LimitOrderStatus.PENDING))
                .willReturn(List.of(expired));

        fire(new BigDecimal("100"));

        verify(filler, never()).fill(anyLong(), any());
        assertThat(registry.get("limit_order.expired_on_sight.count").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("filler 한 건 실패해도 다른 후보는 계속 처리")
    void resilient_per_candidate() {
        LimitOrder ok1 = pending(OrderSide.BUY, "100", OffsetDateTime.now().plusDays(1));
        LimitOrder fail = pending(OrderSide.BUY, "100", OffsetDateTime.now().plusDays(1));
        LimitOrder ok2 = pending(OrderSide.BUY, "100", OffsetDateTime.now().plusDays(1));
        given(limitOrderRepository.findByTickerAndStatus("AAPL", LimitOrderStatus.PENDING))
                .willReturn(List.of(ok1, fail, ok2));
        final int[] cnt = { 0 };
        willAnswer(inv -> {
            if (++cnt[0] == 2) throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
            return null;
        }).given(filler).fill(any(), any());

        fire(new BigDecimal("95"));

        verify(filler, times(3)).fill(any(), any());
        assertThat(registry.get("limit_order.candidate.count").counter().count()).isEqualTo(3);
        assertThat(registry.get("limit_order.filled.count").counter().count()).isEqualTo(2);
    }
}
