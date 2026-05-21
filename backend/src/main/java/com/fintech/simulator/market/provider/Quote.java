package com.fintech.simulator.market.provider;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Provider 중립 시세 스냅샷.
 * - price: 종목 통화 단위 (KRW/USD)
 * - prevClose: 전일 종가 (등락 계산용, null 가능)
 */
public record Quote(
        String ticker,
        BigDecimal price,
        BigDecimal prevClose,
        Instant timestamp
) {}
