package com.fintech.simulator.risk.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskCalculatorTest {

    @Test
    @DisplayName("dailyReturns: close 시리즈 → 단순 수익률")
    void daily_returns() {
        List<BigDecimal> closes = List.of(bd(100), bd(110), bd(99));
        List<Double> r = RiskCalculator.dailyReturns(closes);
        assertThat(r).hasSize(2);
        assertThat(r.get(0)).isCloseTo(0.10, within(1e-9));
        assertThat(r.get(1)).isCloseTo(-0.10, within(1e-9));
    }

    @Test
    @DisplayName("VaR 95%: 음수 수익률 하위 5% 분위 (양수로 반환)")
    void var_95() {
        // 100건: -0.05, -0.04, ..., -0.01, 0.00, ..., 0.04, 0.05
        List<Double> r = new java.util.ArrayList<>();
        for (int i = -50; i < 50; i++) r.add(i / 1000.0);
        // 정렬된 후 인덱스 5 (5%) = -0.045
        BigDecimal var95 = RiskCalculator.valueAtRisk(r, 0.05);
        assertThat(var95.doubleValue()).isBetween(0.040, 0.050);
    }

    @Test
    @DisplayName("Sharpe: 변동성 0이면 0 반환")
    void sharpe_zero_volatility() {
        List<Double> r = List.of(0.01, 0.01, 0.01);
        assertThat(RiskCalculator.sharpeRatio(r, 0.0)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Beta: 시장과 동일 시리즈면 1.0")
    void beta_identical() {
        List<Double> r = List.of(0.01, -0.02, 0.03, -0.01, 0.02);
        assertThat(RiskCalculator.beta(r, r).doubleValue()).isCloseTo(1.0, within(1e-6));
    }

    @Test
    @DisplayName("MDD: 단순 자산 곡선 검증")
    void mdd_basic() {
        // 100 → 120 → 60 → 80 : peak 120, trough 60, MDD = (120-60)/120 = 0.5
        List<BigDecimal> equity = List.of(bd(100), bd(120), bd(60), bd(80));
        assertThat(RiskCalculator.maxDrawdown(equity)).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("topConcentration: 가장 큰 비중 반환")
    void top_concentration() {
        assertThat(RiskCalculator.topConcentration(List.of(0.5, 0.3, 0.2)))
                .isEqualByComparingTo("0.5");
    }

    private static BigDecimal bd(double v) { return BigDecimal.valueOf(v); }
    private static org.assertj.core.data.Offset<Double> within(double v) { return org.assertj.core.data.Offset.offset(v); }
}
