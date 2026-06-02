package com.fintech.simulator.market.dto;

import com.fintech.simulator.market.domain.IntradayCandle;

import java.math.BigDecimal;

/**
 * 분봉 한 점. lightweight-charts intraday 는 time 을 UNIX epoch seconds(UTC)로 받는다.
 */
public record IntradayPoint(
        long time,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume
) {
    public static IntradayPoint from(IntradayCandle c) {
        return new IntradayPoint(
                c.getBucketTs().toEpochSecond(),
                c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume());
    }
}
