package com.fintech.simulator.fx;

import java.math.BigDecimal;

/**
 * 환율 공급자. D15에서 ExchangeRate-API 연동 구현체로 교체.
 * - rate("USD", "KRW") → 1 USD가 몇 KRW인지
 * - 동일 통화 또는 KRW base는 1
 */
public interface FxRateProvider {
    BigDecimal rate(String baseCurrency, String quoteCurrency);
}
