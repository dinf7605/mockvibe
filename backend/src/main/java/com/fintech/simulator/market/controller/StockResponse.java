package com.fintech.simulator.market.controller;

import com.fintech.simulator.market.domain.Stock;

import java.math.BigDecimal;

public record StockResponse(
        String ticker,
        String market,
        String currency,
        String companyName,
        String sector,
        String region,
        BigDecimal currentPrice,
        BigDecimal tickSize,
        boolean isActive
) {
    public static StockResponse from(Stock s) {
        return new StockResponse(
                s.getTicker(),
                s.getMarket(),
                s.getCurrency(),
                s.getCompanyName(),
                s.getSector(),
                s.getRegion(),
                s.getCurrentPrice(),
                s.getTickSize(),
                s.isActive()
        );
    }
}
