package com.fintech.simulator.trading.domain;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 지정가 예약 주문.
 *
 * 체결 매칭 (D22 LimitOrderProcessor):
 *  - BUY:  현재가 <= target_price 도달 시 체결
 *  - SELL: 현재가 >= target_price 도달 시 체결
 *
 * 상태 전이: PENDING → {FILLED | CANCELLED | EXPIRED} (불가역)
 */
@Entity
@Table(name = "LIMIT_ORDERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LimitOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "limit_order_id")
    private Long limitOrderId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 10, nullable = false)
    private OrderSide orderType;

    @Column(name = "target_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal targetPrice;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private LimitOrderStatus status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "filled_at")
    private OffsetDateTime filledAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "filled_order_id")
    private Long filledOrderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private LimitOrder(String userId, String ticker, OrderSide side,
                       BigDecimal targetPrice, BigDecimal quantity, OffsetDateTime expiresAt) {
        this.userId = userId;
        this.ticker = ticker;
        this.orderType = side;
        this.targetPrice = targetPrice;
        this.quantity = quantity;
        this.status = LimitOrderStatus.PENDING;
        this.expiresAt = expiresAt;
        this.createdAt = OffsetDateTime.now();
    }

    public static LimitOrder register(String userId, String ticker, OrderSide side,
                                      BigDecimal targetPrice, BigDecimal quantity,
                                      OffsetDateTime expiresAt) {
        return new LimitOrder(userId, ticker, side, targetPrice, quantity, expiresAt);
    }

    public void markFilled(Long orderId) {
        ensurePending();
        this.status = LimitOrderStatus.FILLED;
        this.filledOrderId = orderId;
        this.filledAt = OffsetDateTime.now();
    }

    public void cancel() {
        ensurePending();
        this.status = LimitOrderStatus.CANCELLED;
        this.cancelledAt = OffsetDateTime.now();
    }

    public void expire() {
        ensurePending();
        this.status = LimitOrderStatus.EXPIRED;
    }

    public boolean matches(BigDecimal currentPrice) {
        if (status != LimitOrderStatus.PENDING) return false;
        return orderType == OrderSide.BUY
                ? currentPrice.compareTo(targetPrice) <= 0
                : currentPrice.compareTo(targetPrice) >= 0;
    }

    private void ensurePending() {
        if (status != LimitOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.LIMIT_ORDER_NOT_PENDING);
        }
    }
}
