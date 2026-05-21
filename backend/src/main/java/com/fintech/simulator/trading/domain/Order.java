package com.fintech.simulator.trading.domain;

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
 * 체결 완료된 거래 내역 (불변, ADR-001).
 * 정정·취소 없음 — 한 번 INSERT되면 그대로 유지.
 */
@Entity
@Table(name = "ORDERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 10, nullable = false)
    private OrderSide orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_method", length = 10, nullable = false)
    private OrderMethod orderMethod;

    @Column(name = "price", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;          // 체결가 (종목 통화)

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "fx_rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal fxRate;         // USD→KRW (KRW 종목은 1)

    @Column(name = "fee", precision = 18, scale = 4, nullable = false)
    private BigDecimal fee;            // 수수료 (KRW 환산)

    @Column(name = "total_amount_krw", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmountKrw; // price * quantity * fx_rate + fee

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private Order(String userId, String ticker, OrderSide side, OrderMethod method,
                  BigDecimal price, BigDecimal quantity,
                  BigDecimal fxRate, BigDecimal fee, BigDecimal totalAmountKrw) {
        this.userId = userId;
        this.ticker = ticker;
        this.orderType = side;
        this.orderMethod = method;
        this.price = price;
        this.quantity = quantity;
        this.fxRate = fxRate;
        this.fee = fee;
        this.totalAmountKrw = totalAmountKrw;
        this.createdAt = OffsetDateTime.now();
    }

    public static Order marketBuy(String userId, String ticker,
                                  BigDecimal price, BigDecimal quantity,
                                  BigDecimal fxRate, BigDecimal fee, BigDecimal totalAmountKrw) {
        return new Order(userId, ticker, OrderSide.BUY, OrderMethod.MARKET,
                price, quantity, fxRate, fee, totalAmountKrw);
    }

    public static Order marketSell(String userId, String ticker,
                                   BigDecimal price, BigDecimal quantity,
                                   BigDecimal fxRate, BigDecimal fee, BigDecimal totalAmountKrw) {
        return new Order(userId, ticker, OrderSide.SELL, OrderMethod.MARKET,
                price, quantity, fxRate, fee, totalAmountKrw);
    }

    public static Order limitBuy(String userId, String ticker,
                                 BigDecimal price, BigDecimal quantity,
                                 BigDecimal fxRate, BigDecimal fee, BigDecimal totalAmountKrw) {
        return new Order(userId, ticker, OrderSide.BUY, OrderMethod.LIMIT,
                price, quantity, fxRate, fee, totalAmountKrw);
    }

    public static Order limitSell(String userId, String ticker,
                                  BigDecimal price, BigDecimal quantity,
                                  BigDecimal fxRate, BigDecimal fee, BigDecimal totalAmountKrw) {
        return new Order(userId, ticker, OrderSide.SELL, OrderMethod.LIMIT,
                price, quantity, fxRate, fee, totalAmountKrw);
    }
}
