package com.fintech.simulator.backtest.strategy;

import com.fintech.simulator.market.domain.PriceHistory;
import org.springframework.stereotype.Component;

import java.util.List;

/** 첫날 매수, 마지막 날까지 보유 (단순 buy & hold). */
@Component
public class BuyAndHoldStrategy implements Strategy {
    @Override public String name() { return "BUY_AND_HOLD"; }

    @Override
    public Signal evaluate(int index, List<PriceHistory> history, boolean inPosition) {
        return !inPosition && index == 0 ? Signal.BUY : Signal.HOLD;
    }
}
