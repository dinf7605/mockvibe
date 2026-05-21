package com.fintech.simulator.market.provider.finnhub;

import java.math.BigDecimal;
import java.util.List;

/**
 * Finnhub trade 메시지:
 * <pre>
 * {"type":"trade","data":[{"s":"AAPL","p":225.10,"t":1234567890123,"v":100}, ...]}
 * </pre>
 */
public record FinnhubTradeMessage(
        String type,
        List<Trade> data
) {
    public record Trade(
            String s,        // symbol
            BigDecimal p,    // price
            Long t,          // unix epoch milliseconds
            BigDecimal v     // volume
    ) {}
}
