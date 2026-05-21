package com.fintech.simulator.risk.service;

import com.fintech.simulator.fx.FxRateProvider;
import com.fintech.simulator.market.domain.PriceHistory;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.repository.PriceHistoryRepository;
import com.fintech.simulator.market.repository.StockRepository;
import com.fintech.simulator.portfolio.domain.Holding;
import com.fintech.simulator.portfolio.repository.HoldingRepository;
import com.fintech.simulator.risk.calculator.RiskCalculator;
import com.fintech.simulator.risk.dto.RiskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포트폴리오 리스크 스냅샷 계산.
 *
 * - 일별 포트폴리오 가치 곡선(KRW 환산) 구성 → 수익률 → VaR/Sharpe/MDD
 * - 시장 벤치마크: KOSPI(005930 삼성전자 대체 사용 — 실 KOSPI ETF 적재는 후속)
 * - 집중도: 보유 종목별 비중 + 임계치(>40%) 경고
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskService {

    private static final int LOOKBACK_DAYS = 252;
    private static final BigDecimal CONCENTRATION_THRESHOLD = new BigDecimal("0.40");
    private static final BigDecimal REGION_THRESHOLD = new BigDecimal("0.70");
    private static final String BENCHMARK_TICKER_KR = "005930";  // 임시: 후속에 KOSPI ETF로 교체

    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final FxRateProvider fxRateProvider;

    public RiskResponse compute(String userId) {
        List<Holding> holdings = holdingRepository.findAllByUserId(userId).stream()
                .filter(h -> h.getQuantity().signum() > 0).toList();
        if (holdings.isEmpty()) return empty();

        Map<String, Stock> stockMap = stockRepository.findAllById(
                holdings.stream().map(Holding::getTicker).toList())
                .stream().collect(Collectors.toMap(Stock::getTicker, s -> s));

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(LOOKBACK_DAYS + 30);

        // 일별 포트폴리오 가치 (KRW) — 각 종목 close × qty × fx, 동일 거래일끼리 합산
        Map<LocalDate, BigDecimal> equityByDate = new java.util.TreeMap<>();
        Map<String, List<BigDecimal>> closesByTicker = new HashMap<>();

        for (Holding h : holdings) {
            Stock s = stockMap.get(h.getTicker());
            if (s == null) continue;
            BigDecimal fx = fxRateProvider.rate(s.getCurrency(), "KRW");
            BigDecimal qty = h.getQuantity();

            List<PriceHistory> ph = priceHistoryRepository
                    .findByTickerAndTradeDateBetweenOrderByTradeDateAsc(h.getTicker(), from, to);
            List<BigDecimal> closes = new ArrayList<>(ph.size());
            for (PriceHistory p : ph) {
                BigDecimal valueKrw = p.getClose().multiply(qty).multiply(fx);
                equityByDate.merge(p.getTradeDate(), valueKrw, BigDecimal::add);
                closes.add(p.getClose());
            }
            closesByTicker.put(h.getTicker(), closes);
        }

        List<BigDecimal> equityCurve = new ArrayList<>(equityByDate.values());
        List<Double> portfolioReturns = RiskCalculator.dailyReturns(equityCurve);

        // 벤치마크 수익률 (KR)
        List<PriceHistory> bench = priceHistoryRepository
                .findByTickerAndTradeDateBetweenOrderByTradeDateAsc(BENCHMARK_TICKER_KR, from, to);
        List<Double> marketReturns = RiskCalculator.dailyReturns(
                bench.stream().map(PriceHistory::getClose).toList());

        BigDecimal var95 = RiskCalculator.valueAtRisk(portfolioReturns, 0.05);
        BigDecimal var99 = RiskCalculator.valueAtRisk(portfolioReturns, 0.01);
        BigDecimal sharpe = RiskCalculator.sharpeRatio(portfolioReturns, RiskCalculator.DEFAULT_RF_ANNUAL);
        BigDecimal beta = RiskCalculator.beta(portfolioReturns, marketReturns);
        BigDecimal mdd = RiskCalculator.maxDrawdown(equityCurve);

        // 비중 (현재 평가금액 기준 — 마지막 날짜)
        BigDecimal total = equityCurve.isEmpty() ? BigDecimal.ZERO : equityCurve.get(equityCurve.size() - 1);
        Map<String, BigDecimal> regionShare = new HashMap<>();
        Map<String, BigDecimal> sectorShare = new HashMap<>();
        List<Double> weights = new ArrayList<>();

        for (Holding h : holdings) {
            Stock s = stockMap.get(h.getTicker());
            if (s == null) continue;
            BigDecimal fx = fxRateProvider.rate(s.getCurrency(), "KRW");
            // 마지막 close 사용
            List<BigDecimal> closes = closesByTicker.get(h.getTicker());
            BigDecimal lastClose = closes == null || closes.isEmpty() ? BigDecimal.ZERO : closes.get(closes.size() - 1);
            BigDecimal value = lastClose.multiply(h.getQuantity()).multiply(fx);
            BigDecimal weight = total.signum() == 0 ? BigDecimal.ZERO
                    : value.divide(total, 6, RoundingMode.HALF_UP);

            regionShare.merge(s.getRegion() == null ? "?" : s.getRegion(), weight, BigDecimal::add);
            sectorShare.merge(s.getSector() == null ? "?" : s.getSector(), weight, BigDecimal::add);
            weights.add(weight.doubleValue());
        }

        BigDecimal concentration = RiskCalculator.topConcentration(weights);

        List<String> warnings = new ArrayList<>();
        if (concentration.compareTo(CONCENTRATION_THRESHOLD) > 0)
            warnings.add(String.format("단일 종목 집중도가 %.1f%%로 권장(40%%) 초과", concentration.doubleValue() * 100));
        regionShare.forEach((k, v) -> {
            if (v.compareTo(REGION_THRESHOLD) > 0)
                warnings.add(String.format("%s 지역 비중이 %.1f%%로 권장(70%%) 초과", k, v.doubleValue() * 100));
        });

        return new RiskResponse(var95, var99, sharpe, beta, mdd, concentration,
                regionShare, sectorShare, warnings);
    }

    private RiskResponse empty() {
        return new RiskResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                Map.of(), Map.of(), List.of("보유 종목이 없어 리스크 지표를 계산할 수 없습니다."));
    }
}
