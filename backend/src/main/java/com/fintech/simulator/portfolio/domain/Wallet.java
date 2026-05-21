package com.fintech.simulator.portfolio.domain;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "WALLET")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Column(name = "user_id", length = 50, nullable = false, unique = true)
    private String userId;

    @Column(name = "cash_balance", precision = 18, scale = 2, nullable = false)
    private BigDecimal cashBalance;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private Wallet(String userId, BigDecimal initialBalance) {
        this.userId = userId;
        this.cashBalance = initialBalance;
        this.updatedAt = OffsetDateTime.now();
    }

    /** 가입 시 시드머니가 주어진 신규 지갑 생성 */
    public static Wallet openWith(String userId, BigDecimal seedMoney) {
        return new Wallet(userId, seedMoney);
    }

    /** 매수 시 차감 — D09에서 사용 */
    public void withdraw(BigDecimal amount) {
        if (cashBalance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.cashBalance = cashBalance.subtract(amount);
        this.updatedAt = OffsetDateTime.now();
    }

    /** 매도 시 입금 — D10에서 사용 */
    public void deposit(BigDecimal amount) {
        this.cashBalance = cashBalance.add(amount);
        this.updatedAt = OffsetDateTime.now();
    }
}
