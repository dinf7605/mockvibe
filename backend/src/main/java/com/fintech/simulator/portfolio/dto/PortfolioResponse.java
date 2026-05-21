package com.fintech.simulator.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        BigDecimal cashBalanceKrw,
        BigDecimal holdingValueKrw,
        BigDecimal totalAssetKrw,
        BigDecimal totalCostKrw,
        BigDecimal totalPnlKrw,
        BigDecimal totalPnlPct,
        List<HoldingItem> holdings,
        RegionShare regionShare
) {
    public record HoldingItem(
            String ticker,
            String companyName,
            String market,
            String currency,
            BigDecimal quantity,
            BigDecimal averagePriceKrw,
            BigDecimal currentPriceKrw,    // 시장 통화 가격을 KRW로 환산한 값
            BigDecimal evaluationKrw,
            BigDecimal pnlKrw,
            BigDecimal pnlPct
    ) {}

    public record RegionShare(BigDecimal kr, BigDecimal us, BigDecimal cash) {}
}
