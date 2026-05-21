package com.fintech.simulator.trading;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * 매매 수수료율 (PRD FR-3.6).
 * - krxFeeRate: 0.00015 (0.015%)
 * - usFeeRate:  0.0025  (0.25%)
 */
@ConfigurationProperties(prefix = "app.trading")
public record TradingProperties(
        BigDecimal krxFeeRate,
        BigDecimal usFeeRate
) {
    public TradingProperties {
        if (krxFeeRate == null) krxFeeRate = new BigDecimal("0.00015");
        if (usFeeRate  == null) usFeeRate  = new BigDecimal("0.0025");
    }

    public BigDecimal feeRateFor(String currency) {
        return "USD".equals(currency) ? usFeeRate : krxFeeRate;
    }
}
