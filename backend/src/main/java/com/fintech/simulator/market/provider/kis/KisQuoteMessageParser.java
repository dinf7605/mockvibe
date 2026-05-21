package com.fintech.simulator.market.provider.kis;

import com.fintech.simulator.market.provider.Quote;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * KIS 실시간 체결 메시지 파서.
 *
 * 메시지 포맷 (pipe-delimited):
 *   "0|H0STCNT0|001|005930^123456^78000^...^"
 *   - [0] 암호화여부 (0=평문)
 *   - [1] tr_id (H0STCNT0=주식체결)
 *   - [2] 데이터 건수
 *   - [3] 실제 데이터 (caret-delimited): ticker^time^price^...
 *
 * 본 파서는 핵심 필드(ticker, price)만 추출. 다른 필드는 D26 이상에서 확장.
 */
@Slf4j
public final class KisQuoteMessageParser {

    private static final String STOCK_EXEC_TR_ID = "H0STCNT0";

    private KisQuoteMessageParser() {}

    public static Optional<Quote> parse(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String[] parts = raw.split("\\|", 4);
        if (parts.length < 4) return Optional.empty();
        if (!STOCK_EXEC_TR_ID.equals(parts[1])) return Optional.empty();

        String[] fields = parts[3].split("\\^");
        if (fields.length < 3) return Optional.empty();

        try {
            String ticker = fields[0];
            BigDecimal price = new BigDecimal(fields[2]);
            return Optional.of(new Quote(ticker, price, null, Instant.now()));
        } catch (NumberFormatException e) {
            log.debug("KIS quote parse failed: {}", raw);
            return Optional.empty();
        }
    }
}
