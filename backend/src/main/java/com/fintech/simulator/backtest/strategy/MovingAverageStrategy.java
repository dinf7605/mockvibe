package com.fintech.simulator.backtest.strategy;

import com.fintech.simulator.market.domain.PriceHistory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * MA(20) — 가격이 20일 이동평균을 상향 돌파 시 BUY, 하향 돌파 시 SELL.
 */
@Component
public class MovingAverageStrategy implements Strategy {
    private static final int WINDOW = 20;

    @Override public String name() { return "MOVING_AVERAGE_20"; }

    @Override
    public Signal evaluate(int index, List<PriceHistory> history, boolean inPosition) {
        if (index < WINDOW) return Signal.HOLD;

        double maPrev = avg(history, index - WINDOW, index);    // i-WINDOW .. i-1
        double maCurr = avg(history, index - WINDOW + 1, index + 1);
        double pricePrev = history.get(index - 1).getClose().doubleValue();
        double priceCurr = history.get(index).getClose().doubleValue();

        boolean crossedUp   = pricePrev <= maPrev && priceCurr > maCurr;
        boolean crossedDown = pricePrev >= maPrev && priceCurr < maCurr;

        if (!inPosition && crossedUp)  return Signal.BUY;
        if (inPosition  && crossedDown) return Signal.SELL;
        return Signal.HOLD;
    }

    private double avg(List<PriceHistory> h, int from, int toExclusive) {
        double sum = 0;
        for (int i = from; i < toExclusive; i++) sum += h.get(i).getClose().doubleValue();
        return sum / (toExclusive - from);
    }

    @SuppressWarnings("unused")
    private BigDecimal nowPrice(PriceHistory p) { return p.getClose(); }
}
