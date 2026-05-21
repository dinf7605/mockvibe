package com.fintech.simulator.portfolio.domain;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

/**
 * 보유 종목.
 * - (user_id, ticker) 유일
 * - 낙관적 락 @Version — 동시 매매 시 ObjectOptimisticLockingFailureException 발생 후 재시도 정책
 * - average_price_krw: 미국 종목도 KRW 환산값으로 통일 저장 (ADR-001)
 */
@Entity
@Table(name = "HOLDINGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holding_id")
    private Long holdingId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    @Column(name = "quantity", precision = 18, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "average_price_krw", precision = 18, scale = 2, nullable = false)
    private BigDecimal averagePriceKrw;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private Holding(String userId, String ticker, BigDecimal quantity, BigDecimal averagePriceKrw) {
        this.userId = userId;
        this.ticker = ticker;
        this.quantity = quantity;
        this.averagePriceKrw = averagePriceKrw;
        this.updatedAt = OffsetDateTime.now();
    }

    public static Holding newPosition(String userId, String ticker, BigDecimal quantity, BigDecimal priceKrw) {
        return new Holding(userId, ticker, quantity, priceKrw);
    }

    /** 매수 시 평균단가 가중 평균 갱신 */
    public void addBuy(BigDecimal addQty, BigDecimal buyPriceKrw) {
        if (addQty == null || addQty.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        BigDecimal totalCost = this.averagePriceKrw.multiply(this.quantity)
                .add(buyPriceKrw.multiply(addQty));
        BigDecimal newQty = this.quantity.add(addQty);
        this.averagePriceKrw = totalCost.divide(newQty, 2, RoundingMode.HALF_UP);
        this.quantity = newQty;
        this.updatedAt = OffsetDateTime.now();
    }

    /** 매도 시 수량 차감 (평균단가는 유지) — D10에서 사용 */
    public void reduceForSell(BigDecimal sellQty) {
        if (sellQty == null || sellQty.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        if (this.quantity.compareTo(sellQty) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_HOLDINGS);
        }
        this.quantity = this.quantity.subtract(sellQty);
        this.updatedAt = OffsetDateTime.now();
    }
}
