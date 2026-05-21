package com.fintech.simulator.backtest.engine;

import com.fintech.simulator.backtest.strategy.Strategy;
import com.fintech.simulator.backtest.strategy.Strategy.Signal;
import com.fintech.simulator.market.domain.PriceHistory;
import com.fintech.simulator.risk.calculator.RiskCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 백테스트 엔진 (In-Memory).
 *
 * 정책:
 *   - 신호 시점의 close에 매수/매도 (slippage·수수료는 단순화 — 향후 확장)
 *   - BUY: 가용 자본 전액으로 정수 주식 매수 (남은 현금은 보유)
 *   - SELL: 보유 전량 매도
 *   - 결과: equity curve, 매매 시점, 누적수익률·MDD·Sharpe·거래횟수·승률
 *
 * 목표: 1년치 1종목 3초 이내 (PRD NFR-1)
 */
@Component
public class BacktestEngine {

    public Result run(Strategy strategy, List<PriceHistory> history, BigDecimal initialCapital) {
        if (history == null || history.size() < 2) {
            throw new IllegalArgumentException("Insufficient price history");
        }

        BigDecimal cash = initialCapital;
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal entryPrice = BigDecimal.ZERO;
        boolean inPosition = false;
        int wins = 0, losses = 0;

        List<EquityPoint> curve = new ArrayList<>(history.size());
        List<TradePoint> trades = new ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            PriceHistory p = history.get(i);
            BigDecimal price = p.getClose();

            Signal signal = strategy.evaluate(i, history, inPosition);
            if (signal == Signal.BUY && !inPosition) {
                long n = cash.divide(price, 0, RoundingMode.FLOOR).longValueExact();
                if (n > 0) {
                    BigDecimal cost = price.multiply(BigDecimal.valueOf(n));
                    cash = cash.subtract(cost);
                    shares = BigDecimal.valueOf(n);
                    entryPrice = price;
                    inPosition = true;
                    trades.add(new TradePoint(p.getTradeDate(), "BUY", price, n));
                }
            } else if (signal == Signal.SELL && inPosition) {
                BigDecimal proceeds = price.multiply(shares);
                cash = cash.add(proceeds);
                if (price.compareTo(entryPrice) > 0) wins++; else losses++;
                trades.add(new TradePoint(p.getTradeDate(), "SELL", price, shares.longValue()));
                shares = BigDecimal.ZERO;
                inPosition = false;
            }

            BigDecimal equity = cash.add(shares.multiply(price));
            curve.add(new EquityPoint(p.getTradeDate(), equity));
        }

        // 마지막에 보유 중이면 평가 (실현 손익엔 미포함 — 승률 계산에서 제외)
        BigDecimal finalValue = curve.get(curve.size() - 1).equity();
        BigDecimal totalReturn = finalValue.subtract(initialCapital)
                .divide(initialCapital, 6, RoundingMode.HALF_UP);

        List<BigDecimal> equityValues = curve.stream().map(EquityPoint::equity).toList();
        BigDecimal mdd = RiskCalculator.maxDrawdown(equityValues);
        BigDecimal sharpe = RiskCalculator.sharpeRatio(
                RiskCalculator.dailyReturns(equityValues), RiskCalculator.DEFAULT_RF_ANNUAL);

        int tradeCount = wins + losses;
        BigDecimal winRate = tradeCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf((double) wins / tradeCount).setScale(4, RoundingMode.HALF_UP);

        return new Result(finalValue, totalReturn, mdd, sharpe, tradeCount, winRate, curve, trades);
    }

    public record EquityPoint(LocalDate date, BigDecimal equity) {}
    public record TradePoint(LocalDate date, String side, BigDecimal price, long quantity) {}
    public record Result(
            BigDecimal finalValue, BigDecimal totalReturn, BigDecimal mdd, BigDecimal sharpe,
            int tradeCount, BigDecimal winRate,
            List<EquityPoint> equityCurve, List<TradePoint> trades
    ) {}
}
