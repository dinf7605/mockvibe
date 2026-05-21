package com.fintech.simulator.market.cache;

import com.fintech.simulator.market.provider.Quote;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-process 시세 캐시 (ConcurrentHashMap).
 * - 외부 Provider WebSocket이 갱신 → 캐시 저장 → STOMP 브로드캐스트 (D16)
 * - PRD §10: N-to-N 외부 호출 방지를 위한 단일 진실 소스
 */
@Component
public class PriceCache {

    private final ConcurrentMap<String, Quote> store = new ConcurrentHashMap<>();

    public Optional<Quote> get(String ticker) {
        return Optional.ofNullable(store.get(ticker));
    }

    public void put(Quote quote) {
        store.put(quote.ticker(), quote);
    }

    public int size() {
        return store.size();
    }

    /** 테스트/관리자용 */
    public void clear() {
        store.clear();
    }
}
