package com.fintech.simulator.backtest.dto;

import com.fintech.simulator.backtest.engine.BacktestEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BacktestResponse(
        Long runId,
        String strategy,
        String ticker,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal initialCapital,
        BigDecimal finalValue,
        BigDecimal totalReturn,
        BigDecimal mdd,
        BigDecimal sharpe,
        int tradeCount,
        BigDecimal winRate,
        List<EquityPoint> equityCurve,
        List<TradePoint> trades
) {
    public record EquityPoint(LocalDate date, BigDecimal equity) {
        static EquityPoint from(BacktestEngine.EquityPoint p) { return new EquityPoint(p.date(), p.equity()); }
    }
    public record TradePoint(LocalDate date, String side, BigDecimal price, long quantity) {
        static TradePoint from(BacktestEngine.TradePoint p) { return new TradePoint(p.date(), p.side(), p.price(), p.quantity()); }
    }

    public static BacktestResponse of(Long runId, BacktestRequest req,
                                      BacktestEngine.Result r) {
        return new BacktestResponse(
                runId, req.strategy(), req.ticker(), req.startDate(), req.endDate(),
                req.initialCapital(), r.finalValue(), r.totalReturn(), r.mdd(), r.sharpe(),
                r.tradeCount(), r.winRate(),
                r.equityCurve().stream().map(EquityPoint::from).toList(),
                r.trades().stream().map(TradePoint::from).toList()
        );
    }
}
