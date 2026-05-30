package com.fintech.simulator.alert.domain;

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
 * 가격 알림 한 건. 시세 갱신마다 {@link #matches(BigDecimal)} 로 도달 여부를 판정하고,
 * 도달 시 {@link #trigger(BigDecimal)} 로 TRIGGERED 상태가 된다 (1회성).
 */
@Entity
@Table(name = "PRICE_ALERT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 10, nullable = false)
    private AlertDirection direction;

    @Column(name = "target_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private AlertStatus status;

    @Column(name = "triggered_price", precision = 18, scale = 4)
    private BigDecimal triggeredPrice;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "triggered_at")
    private OffsetDateTime triggeredAt;

    private PriceAlert(String userId, String ticker, AlertDirection direction, BigDecimal targetPrice) {
        this.userId = userId;
        this.ticker = ticker;
        this.direction = direction;
        this.targetPrice = targetPrice;
        this.status = AlertStatus.ACTIVE;
        this.createdAt = OffsetDateTime.now();
    }

    public static PriceAlert create(String userId, String ticker,
                                    AlertDirection direction, BigDecimal targetPrice) {
        return new PriceAlert(userId, ticker, direction, targetPrice);
    }

    /** ACTIVE 상태에서만, 방향에 따라 목표가 도달 여부 판정. */
    public boolean matches(BigDecimal currentPrice) {
        if (status != AlertStatus.ACTIVE || currentPrice == null) return false;
        return direction == AlertDirection.ABOVE
                ? currentPrice.compareTo(targetPrice) >= 0
                : currentPrice.compareTo(targetPrice) <= 0;
    }

    /** 목표가 도달 — TRIGGERED 로 전이 (1회성). */
    public void trigger(BigDecimal currentPrice) {
        if (status != AlertStatus.ACTIVE) return;
        this.status = AlertStatus.TRIGGERED;
        this.triggeredPrice = currentPrice;
        this.triggeredAt = OffsetDateTime.now();
    }

    /** 사용자 취소 — ACTIVE 일 때만 의미 있음. */
    public void cancel() {
        this.status = AlertStatus.CANCELLED;
    }
}
