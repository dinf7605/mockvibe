package com.fintech.simulator.fx;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 임시 환율 fallback. ApiFxRateProvider Bean이 등록되면 비활성.
 * 외부 환율 API 미설정·장애 환경에서 매매가 멈추지 않도록 안전망.
 */
@Component
@ConditionalOnMissingBean(FxRateProvider.class)
public class StubFxRateProvider implements FxRateProvider {

    private static final BigDecimal USD_TO_KRW = new BigDecimal("1380.00");

    @Override
    public BigDecimal rate(String base, String quote) {
        if (base == null || quote == null || base.equals(quote)) {
            return BigDecimal.ONE;
        }
        if ("USD".equals(base) && "KRW".equals(quote)) return USD_TO_KRW;
        if ("KRW".equals(base) && "USD".equals(quote)) {
            return BigDecimal.ONE.divide(USD_TO_KRW, 6, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ONE;
    }
}
