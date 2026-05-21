package com.fintech.simulator.market.provider.finnhub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.simulator.market.provider.Quote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Finnhub WebSocket 메시지(JSON) → Quote 리스트 변환.
 * - type=ping 등 trade 외 메시지는 빈 리스트 반환
 * - 동일 종목 다건 trade가 한 메시지에 올 수 있음 → 모두 펼침
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinnhubMessageParser {

    private static final String TRADE_TYPE = "trade";

    private final ObjectMapper objectMapper;

    public List<Quote> parse(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            FinnhubTradeMessage msg = objectMapper.readValue(raw, FinnhubTradeMessage.class);
            if (!TRADE_TYPE.equals(msg.type()) || msg.data() == null) return List.of();
            return msg.data().stream()
                    .filter(t -> t != null && t.s() != null && t.p() != null)
                    .map(t -> new Quote(t.s(), t.p(), null,
                            t.t() != null ? Instant.ofEpochMilli(t.t()) : Instant.now()))
                    .toList();
        } catch (JsonProcessingException e) {
            log.debug("Finnhub message parse failed: {}", e.getMessage());
            return List.of();
        }
    }
}
