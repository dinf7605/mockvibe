package com.fintech.simulator.backtest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "BACKTEST_RUNS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BacktestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long runId;

    @Column(name = "user_id", length = 50, nullable = false) private String userId;
    @Column(name = "strategy_name", length = 50, nullable = false) private String strategyName;
    @Lob @Column(name = "strategy_params") private String strategyParams;
    @Column(name = "ticker", length = 20, nullable = false) private String ticker;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date",   nullable = false) private LocalDate endDate;

    @Column(name = "initial_capital", precision = 18, scale = 2, nullable = false) private BigDecimal initialCapital;
    @Column(name = "final_value",     precision = 18, scale = 2, nullable = false) private BigDecimal finalValue;
    @Column(name = "total_return",    precision = 10, scale = 6, nullable = false) private BigDecimal totalReturn;
    @Column(name = "mdd",             precision = 10, scale = 6, nullable = false) private BigDecimal mdd;
    @Column(name = "sharpe",          precision = 10, scale = 6, nullable = false) private BigDecimal sharpe;
    @Column(name = "trade_count",     nullable = false) private Integer tradeCount;
    @Column(name = "win_rate",        precision = 5, scale = 4, nullable = false) private BigDecimal winRate;

    @Lob @Column(name = "result_detail") private String resultDetail;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;

    public static BacktestRun of(String userId, String strategy, String params, String ticker,
                                 LocalDate from, LocalDate to, BigDecimal initial, BigDecimal finalV,
                                 BigDecimal totalReturn, BigDecimal mdd, BigDecimal sharpe,
                                 int tradeCount, BigDecimal winRate, String detail) {
        BacktestRun b = new BacktestRun();
        b.userId = userId; b.strategyName = strategy; b.strategyParams = params;
        b.ticker = ticker; b.startDate = from; b.endDate = to;
        b.initialCapital = initial; b.finalValue = finalV;
        b.totalReturn = totalReturn; b.mdd = mdd; b.sharpe = sharpe;
        b.tradeCount = tradeCount; b.winRate = winRate;
        b.resultDetail = detail; b.createdAt = OffsetDateTime.now();
        return b;
    }
}
