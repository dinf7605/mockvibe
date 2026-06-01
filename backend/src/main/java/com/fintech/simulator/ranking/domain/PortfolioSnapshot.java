package com.fintech.simulator.ranking.domain;

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
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 일별 포트폴리오 자산 스냅샷. (user_id, snapshot_date) UNIQUE — 하루 1행 UPSERT.
 * 수익률 랭킹과 자산 추이 그래프의 단일 데이터 소스.
 */
@Entity
@Table(name = "PORTFOLIO_SNAPSHOT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_asset_krw", precision = 20, scale = 2, nullable = false)
    private BigDecimal totalAssetKrw;

    @Column(name = "cash_krw", precision = 20, scale = 2, nullable = false)
    private BigDecimal cashKrw;

    @Column(name = "holding_krw", precision = 20, scale = 2, nullable = false)
    private BigDecimal holdingKrw;

    @Column(name = "pnl_krw", precision = 20, scale = 2, nullable = false)
    private BigDecimal pnlKrw;

    @Column(name = "return_pct", precision = 12, scale = 4, nullable = false)
    private BigDecimal returnPct;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    private PortfolioSnapshot(String userId, LocalDate date, BigDecimal total, BigDecimal cash,
                              BigDecimal holding, BigDecimal pnl, BigDecimal returnPct) {
        this.userId = userId;
        this.snapshotDate = date;
        this.totalAssetKrw = total;
        this.cashKrw = cash;
        this.holdingKrw = holding;
        this.pnlKrw = pnl;
        this.returnPct = returnPct;
        this.createdAt = OffsetDateTime.now();
    }

    public static PortfolioSnapshot of(String userId, LocalDate date, BigDecimal total, BigDecimal cash,
                                       BigDecimal holding, BigDecimal pnl, BigDecimal returnPct) {
        return new PortfolioSnapshot(userId, date, total, cash, holding, pnl, returnPct);
    }

    /** 같은 날짜 재실행 시 최신 평가로 갱신 (UPSERT update). */
    public void update(BigDecimal total, BigDecimal cash, BigDecimal holding, BigDecimal pnl, BigDecimal returnPct) {
        this.totalAssetKrw = total;
        this.cashKrw = cash;
        this.holdingKrw = holding;
        this.pnlKrw = pnl;
        this.returnPct = returnPct;
    }
}
