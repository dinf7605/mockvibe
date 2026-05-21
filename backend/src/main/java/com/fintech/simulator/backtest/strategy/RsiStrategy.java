package com.fintech.simulator.backtest.strategy;

import com.fintech.simulator.market.domain.PriceHistory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RSI(14) — RSI < 30이면 BUY(과매도), > 70이면 SELL(과매수).
 * 단순 평균(SMA) 기반 RSI.
 */
@Component
public class RsiStrategy implements Strategy {
    private static final int PERIOD = 14;
    private static final double OVERSOLD = 30;
    private static final double OVERBOUGHT = 70;

    @Override public String name() { return "RSI_14"; }

    @Override
    public Signal evaluate(int index, List<PriceHistory> history, boolean inPosition) {
        if (index < PERIOD) return Signal.HOLD;
        double rsi = computeRsi(history, index);
        if (!inPosition && rsi < OVERSOLD)   return Signal.BUY;
        if (inPosition  && rsi > OVERBOUGHT) return Signal.SELL;
        return Signal.HOLD;
    }

    private double computeRsi(List<PriceHistory> h, int index) {
        double gain = 0, loss = 0;
        for (int i = index - PERIOD + 1; i <= index; i++) {
            double prev = h.get(i - 1).getClose().doubleValue();
            double cur  = h.get(i).getClose().doubleValue();
            double diff = cur - prev;
            if (diff >= 0) gain += diff; else loss -= diff;
        }
        double avgGain = gain / PERIOD;
        double avgLoss = loss / PERIOD;
        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}
