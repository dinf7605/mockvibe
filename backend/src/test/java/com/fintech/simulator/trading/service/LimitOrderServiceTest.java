package com.fintech.simulator.trading.service;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.repository.StockRepository;
import com.fintech.simulator.trading.domain.LimitOrder;
import com.fintech.simulator.trading.domain.LimitOrderStatus;
import com.fintech.simulator.trading.domain.OrderSide;
import com.fintech.simulator.trading.dto.LimitOrderRequest;
import com.fintech.simulator.trading.dto.LimitOrderResponse;
import com.fintech.simulator.trading.repository.LimitOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LimitOrderServiceTest {

    @Mock StockRepository stockRepository;
    @Mock LimitOrderRepository limitOrderRepository;
    LimitOrderService service;

    @BeforeEach
    void setUp() {
        service = new LimitOrderService(stockRepository, limitOrderRepository);
        given(limitOrderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("register: 정상 등록 + 기본 30일 만료")
    void register_success() {
        Stock stock = org.mockito.Mockito.mock(Stock.class);
        given(stock.isActive()).willReturn(true);
        given(stockRepository.findById("005930")).willReturn(Optional.of(stock));
        LimitOrderRequest req = new LimitOrderRequest(
                "005930", OrderSide.BUY, new BigDecimal("70000"), BigDecimal.ONE, null);

        LimitOrderResponse r = service.register("u1", req);

        assertThat(r.status()).isEqualTo(LimitOrderStatus.PENDING);
        assertThat(r.targetPrice()).isEqualByComparingTo("70000");
        assertThat(r.expiresAt()).isAfter(OffsetDateTime.now().plusDays(29));
    }

    @Test
    @DisplayName("register: 종목 없음 → STOCK_NOT_FOUND, save 호출 안 됨")
    void register_no_stock() {
        given(stockRepository.findById("ZZZZ")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("u1",
                new LimitOrderRequest("ZZZZ", OrderSide.BUY, BigDecimal.ONE, BigDecimal.ONE, null)))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STOCK_NOT_FOUND);
        verify(limitOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancel: 본인이 아니면 LIMIT_ORDER_FORBIDDEN")
    void cancel_forbidden() {
        LimitOrder owned = LimitOrder.register("owner", "005930", OrderSide.BUY,
                new BigDecimal("70000"), BigDecimal.ONE, OffsetDateTime.now().plusDays(1));
        given(limitOrderRepository.findById(1L)).willReturn(Optional.of(owned));

        assertThatThrownBy(() -> service.cancel("intruder", 1L))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LIMIT_ORDER_FORBIDDEN);
    }

    @Test
    @DisplayName("matches: BUY는 현재가<=target, SELL은 현재가>=target")
    void matches_logic() {
        LimitOrder buy = LimitOrder.register("u", "T", OrderSide.BUY,
                new BigDecimal("100"), BigDecimal.ONE, OffsetDateTime.now().plusDays(1));
        LimitOrder sell = LimitOrder.register("u", "T", OrderSide.SELL,
                new BigDecimal("100"), BigDecimal.ONE, OffsetDateTime.now().plusDays(1));

        assertThat(buy.matches(new BigDecimal("99"))).isTrue();
        assertThat(buy.matches(new BigDecimal("100"))).isTrue();
        assertThat(buy.matches(new BigDecimal("101"))).isFalse();

        assertThat(sell.matches(new BigDecimal("100"))).isTrue();
        assertThat(sell.matches(new BigDecimal("101"))).isTrue();
        assertThat(sell.matches(new BigDecimal("99"))).isFalse();
    }

    @Test
    @DisplayName("이미 처리된 주문 cancel/expire/markFilled → LIMIT_ORDER_NOT_PENDING")
    void state_transition_guard() {
        LimitOrder o = LimitOrder.register("u", "T", OrderSide.BUY,
                new BigDecimal("100"), BigDecimal.ONE, OffsetDateTime.now().plusDays(1));
        o.cancel();
        assertThatThrownBy(o::cancel)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LIMIT_ORDER_NOT_PENDING);
        assertThatThrownBy(() -> o.markFilled(99L))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LIMIT_ORDER_NOT_PENDING);
    }
}
