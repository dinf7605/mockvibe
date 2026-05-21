package com.fintech.simulator.market.controller;

import com.fintech.simulator.market.provider.Quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record PriceResponse(
        String ticker,
        BigDecimal price,
        BigDecimal prevClose,
        BigDecimal changePct,   // (price - prevClose) / prevClose * 100, 소수 둘째 자리
        Instant timestamp
) {
    public static PriceResponse from(Quote q) {
        BigDecimal changePct = (q.prevClose() == null || q.prevClose().signum() == 0)
                ? BigDecimal.ZERO
                : q.price().subtract(q.prevClose())
                        .divide(q.prevClose(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
        return new PriceResponse(q.ticker(), q.price(), q.prevClose(), changePct, q.timestamp());
    }
}
