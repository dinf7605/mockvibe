package com.fintech.simulator.backtest.engine;

import com.fintech.simulator.backtest.strategy.BuyAndHoldStrategy;
import com.fintech.simulator.market.domain.PriceHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BacktestEngineTest {

    private final BacktestEngine engine = new BacktestEngine();

    private List<PriceHistory> history(double... closes) {
        List<PriceHistory> h = new ArrayList<>();
        for (int i = 0; i < closes.length; i++) {
            BigDecimal c = BigDecimal.valueOf(closes[i]);
            h.add(PriceHistory.of("T", LocalDate.now().minusDays(closes.length - i), c, c, c, c, 1000));
        }
        return h;
    }

    @Test
    @DisplayName("BuyAndHold: 첫날 100 → 마지막 150, 누적수익률 50%")
    void buyhold_simple() {
        List<PriceHistory> h = history(100, 110, 120, 130, 150);
        BacktestEngine.Result r = engine.run(new BuyAndHoldStrategy(), h, new BigDecimal("10000"));

        // 자본 10000, 첫날 close=100 → 100주 매수, 마지막 close=150 → 평가 15000
        assertThat(r.finalValue()).isEqualByComparingTo("15000");
        assertThat(r.totalReturn()).isEqualByComparingTo("0.500000");
        assertThat(r.tradeCount()).isEqualTo(0);    // 매도하지 않았으므로 실현 거래 0
        assertThat(r.equityCurve()).hasSize(5);
    }

    @Test
    @DisplayName("BuyAndHold: 하락장에서 MDD 정확")
    void buyhold_mdd() {
        // 100 → 매수 → 50으로 폭락. 자산 10000 → 5000. MDD = (10000-5000)/10000 = 0.5
        List<PriceHistory> h = history(100, 100, 50);
        BacktestEngine.Result r = engine.run(new BuyAndHoldStrategy(), h, new BigDecimal("10000"));
        assertThat(r.mdd().doubleValue()).isGreaterThanOrEqualTo(0.49);
    }
}
