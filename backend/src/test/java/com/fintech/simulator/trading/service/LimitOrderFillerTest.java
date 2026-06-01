package com.fintech.simulator.trading.service;

import com.fintech.simulator.trading.domain.LimitOrder;
import com.fintech.simulator.trading.domain.LimitOrderStatus;
import com.fintech.simulator.trading.domain.OrderSide;
import com.fintech.simulator.notification.service.NotificationService;
import com.fintech.simulator.trading.repository.LimitOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LimitOrderFillerTest {

    @Mock LimitOrderRepository limitOrderRepository;
    @Mock TradingService tradingService;
    @Mock NotificationService notificationService;
    @InjectMocks LimitOrderFiller filler;

    @Test
    @DisplayName("matches 통과 + 체결 → markFilled(orderId) + 상태 FILLED")
    void fill_success() {
        LimitOrder lo = LimitOrder.register("u1", "AAPL", OrderSide.BUY,
                new BigDecimal("100"), BigDecimal.ONE, OffsetDateTime.now().plusDays(1));
        given(limitOrderRepository.findById(1L)).willReturn(Optional.of(lo));
        given(tradingService.fillLimit(any(), any(), any(), any(), any())).willReturn(42L);

        filler.fill(1L, new BigDecimal("95"));

        assertThat(lo.getStatus()).isEqualTo(LimitOrderStatus.FILLED);
        assertThat(lo.getFilledOrderId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("매칭 안 되면 TradingService 호출 안 함 + 상태 그대로")
    void no_match() {
        LimitOrder lo = LimitOrder.register("u1", "AAPL", OrderSide.BUY,
                new BigDecimal("100"), BigDecimal.ONE, OffsetDateTime.now().plusDays(1));
        given(limitOrderRepository.findById(1L)).willReturn(Optional.of(lo));

        filler.fill(1L, new BigDecimal("110"));  // BUY인데 현재가 > target

        verify(tradingService, never()).fillLimit(any(), any(), any(), any(), any());
        assertThat(lo.getStatus()).isEqualTo(LimitOrderStatus.PENDING);
    }
}
