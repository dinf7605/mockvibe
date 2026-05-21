package com.fintech.simulator.market.provider.kis;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KIS WebSocket 동시 구독 종목 관리 (PRD §9.1 한도 41).
 *
 * - subscribe/unsubscribe로 종목 등록·해제
 * - 한도 초과 시 SUBSCRIPTION_LIMIT
 * - 멱등: 이미 구독 중인 종목은 추가 카운트하지 않음
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisSubscriptionManager {

    private final KisProperties props;
    private final Set<String> tickers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * @return true=신규 구독, false=이미 구독 중
     */
    public boolean subscribe(String ticker) {
        synchronized (tickers) {
            if (tickers.contains(ticker)) return false;
            if (tickers.size() >= props.maxSubscriptions()) {
                throw new BusinessException(ErrorCode.SUBSCRIPTION_LIMIT,
                        "KIS WebSocket subscriptions limit: " + props.maxSubscriptions());
            }
            tickers.add(ticker);
            return true;
        }
    }

    /** @return true=실제 해제됨 */
    public boolean unsubscribe(String ticker) {
        return tickers.remove(ticker);
    }

    public int size() { return tickers.size(); }

    public Set<String> snapshot() { return Set.copyOf(tickers); }

    public boolean isSubscribed(String ticker) { return tickers.contains(ticker); }
}
