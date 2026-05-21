package com.fintech.simulator.risk.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 핵심 리스크 지표 계산.
 *
 * 모든 메서드는 stateless / pure function.
 * - returns: 일별 단순 수익률 (close_t / close_{t-1} - 1)
 * - VaR (Historical Simulation): 수익률 분포의 p% 분위수 (음수면 손실)
 * - Sharpe: (E[r] - rf/252) / σ(r) × √252  (연율화)
 * - Beta:  Cov(asset, market) / Var(market)
 * - MDD:   max((peak - trough) / peak)  — 자산 곡선 기반
 */
public final class RiskCalculator {

    private static final int SCALE = 4;
    /** 연간 무위험금리 기본값 (한국 단기채 ~3.5%) */
    public static final double DEFAULT_RF_ANNUAL = 0.035;

    private RiskCalculator() {}

    public static List<Double> dailyReturns(List<BigDecimal> closes) {
        if (closes == null || closes.size() < 2) return List.of();
        List<Double> r = new ArrayList<>(closes.size() - 1);
        for (int i = 1; i < closes.size(); i++) {
            double prev = closes.get(i - 1).doubleValue();
            double cur  = closes.get(i).doubleValue();
            if (prev == 0) continue;
            r.add(cur / prev - 1.0);
        }
        return r;
    }

    /** percentile: 0.05 → 95% VaR (수익률 하위 5%). 반환값은 양수(손실 크기). */
    public static BigDecimal valueAtRisk(List<Double> returns, double percentile) {
        if (returns.isEmpty()) return BigDecimal.ZERO;
        double[] sorted = returns.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        int idx = Math.max(0, Math.min(sorted.length - 1, (int) Math.floor(percentile * sorted.length)));
        double v = sorted[idx];
        return BigDecimal.valueOf(-v).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** 연율화 Sharpe Ratio */
    public static BigDecimal sharpeRatio(List<Double> returns, double rfAnnual) {
        if (returns.size() < 2) return BigDecimal.ZERO;
        double mean = mean(returns);
        double std  = stddev(returns, mean);
        if (std == 0) return BigDecimal.ZERO;
        double dailyRf = rfAnnual / 252.0;
        double sharpe = (mean - dailyRf) / std * Math.sqrt(252);
        return BigDecimal.valueOf(sharpe).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Beta = Cov(asset, market) / Var(market) */
    public static BigDecimal beta(List<Double> assetReturns, List<Double> marketReturns) {
        int n = Math.min(assetReturns.size(), marketReturns.size());
        if (n < 2) return BigDecimal.ZERO;
        double aMean = mean(assetReturns.subList(0, n));
        double mMean = mean(marketReturns.subList(0, n));
        double cov = 0, varM = 0;
        for (int i = 0; i < n; i++) {
            double da = assetReturns.get(i) - aMean;
            double dm = marketReturns.get(i) - mMean;
            cov  += da * dm;
            varM += dm * dm;
        }
        if (varM == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(cov / varM).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Max Drawdown — 자산 곡선(equity)의 최대 하락폭 (0~1, 양수) */
    public static BigDecimal maxDrawdown(List<BigDecimal> equityCurve) {
        if (equityCurve == null || equityCurve.size() < 2) return BigDecimal.ZERO;
        double peak = -Double.MAX_VALUE;
        double maxDd = 0;
        for (BigDecimal v : equityCurve) {
            double x = v.doubleValue();
            if (x > peak) peak = x;
            if (peak > 0) {
                double dd = (peak - x) / peak;
                if (dd > maxDd) maxDd = dd;
            }
        }
        return BigDecimal.valueOf(maxDd).setScale(SCALE, RoundingMode.HALF_UP);
    }

    // ===== private helpers =====
    private static double mean(List<Double> xs) {
        return xs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
    private static double stddev(List<Double> xs, double mean) {
        double s = 0;
        for (double x : xs) { double d = x - mean; s += d * d; }
        return Math.sqrt(s / (xs.size() - 1));   // 표본 표준편차
    }

    /** 보유 비중 리스트 → 가장 큰 비중 (집중도 게이지용). 0~1. */
    public static BigDecimal topConcentration(List<Double> weights) {
        if (weights == null || weights.isEmpty()) return BigDecimal.ZERO;
        double top = Collections.max(Arrays.asList(weights.toArray(Double[]::new)));
        return BigDecimal.valueOf(top).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
