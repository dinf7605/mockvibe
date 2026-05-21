package com.fintech.simulator.trading.scheduler;

import com.fintech.simulator.trading.domain.LimitOrder;
import com.fintech.simulator.trading.domain.LimitOrderStatus;
import com.fintech.simulator.trading.domain.OrderSide;
import com.fintech.simulator.trading.repository.LimitOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LimitOrderExpirySchedulerTest {

    @Mock LimitOrderRepository limitOrderRepository;
    @InjectMocks LimitOrderExpiryScheduler scheduler;

    @Test
    @DisplayName("만료 대상만 EXPIRED 전이, 멱등(이미 처리된 건 silently skip)")
    void expire_targets_and_skip_already_processed() {
        LimitOrder a = LimitOrder.register("u", "AAPL", OrderSide.BUY,
                new BigDecimal("100"), BigDecimal.ONE, OffsetDateTime.now().minusDays(1));
        LimitOrder b = LimitOrder.register("u", "MSFT", OrderSide.SELL,
                new BigDecimal("400"), BigDecimal.ONE, OffsetDateTime.now().minusDays(2));
        b.cancel(); // 다른 스레드가 먼저 취소한 케이스 — expire() 호출 시 LIMIT_ORDER_NOT_PENDING

        given(limitOrderRepository.findByStatusAndExpiresAtLessThan(
                any(LimitOrderStatus.class), any(OffsetDateTime.class)))
                .willReturn(List.of(a, b));

        int n = scheduler.runOnce();

        assertThat(n).isEqualTo(1);
        assertThat(a.getStatus()).isEqualTo(LimitOrderStatus.EXPIRED);
        assertThat(b.getStatus()).isEqualTo(LimitOrderStatus.CANCELLED);  // 보존
    }
}
