package com.fintech.simulator.market.dto;

import com.fintech.simulator.market.domain.PriceHistory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일봉 OHLCV. lightweight-charts 의 CandlestickData 와 호환되는 필드 구성.
 *
 * 클라이언트는 time(LocalDate) 을 'YYYY-MM-DD' 문자열로 직렬화된 형태로 받아
 * 그대로 lightweight-charts time 으로 사용 가능.
 */
public record DailyCandle(
        LocalDate time,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume
) {
    public static DailyCandle from(PriceHistory h) {
        return new DailyCandle(
                h.getTradeDate(),
                h.getOpen(),
                h.getHigh(),
                h.getLow(),
                h.getClose(),
                h.getVolume()
        );
    }
}
