package com.fintech.simulator.market.provider.finnhub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.simulator.market.cache.PriceCache;
import com.fintech.simulator.market.event.PriceUpdatedEvent;
import com.fintech.simulator.market.provider.Quote;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Finnhub WebSocket 클라이언트.
 *
 * - URL: wss://ws.finnhub.io?token=API_KEY
 * - 구독:   {"type":"subscribe","symbol":"AAPL"}
 * - 해제:   {"type":"unsubscribe","symbol":"AAPL"}
 * - 메시지: {"type":"trade","data":[{"s":"AAPL","p":225.1,"t":..., "v":100}, ...]}
 *
 * 동적 구독: 보는 종목만 구독해 무료 티어 동시 구독 부담 최소화 (PRD §10 #3).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.external.finnhub.api-key")
public class FinnhubWebSocketClient {

    private final FinnhubProperties props;
    private final FinnhubMessageParser parser;
    private final PriceCache priceCache;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Set<String> tickers = ConcurrentHashMap.newKeySet();
    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private volatile boolean shuttingDown = false;

    public synchronized void subscribe(String ticker) {
        if (!tickers.add(ticker)) return; // 멱등
        ensureConnected();
        send(Map.of("type", "subscribe", "symbol", ticker));
    }

    public synchronized void unsubscribe(String ticker) {
        if (!tickers.remove(ticker)) return;
        send(Map.of("type", "unsubscribe", "symbol", ticker));
    }

    public int subscriptionCount() { return tickers.size(); }

    private void ensureConnected() {
        if (socket.get() != null) return;
        try {
            URI uri = URI.create(props.wsUrl() + "?token=" + props.apiKey());
            WebSocket ws = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(uri, new Listener())
                    .join();
            socket.set(ws);
            log.info("Finnhub WebSocket connected");
            // 기존 구독 재발송 (재연결 시 상태 복원)
            tickers.forEach(t -> send(Map.of("type", "subscribe", "symbol", t)));
        } catch (Exception e) {
            log.warn("Finnhub WebSocket connect failed: {}", e.getMessage());
        }
    }

    private void send(Map<String, String> payload) {
        WebSocket ws = socket.get();
        if (ws == null) return;
        try {
            ws.sendText(objectMapper.writeValueAsString(payload), true);
        } catch (JsonProcessingException e) {
            log.warn("Finnhub send payload build failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        shuttingDown = true;
        WebSocket ws = socket.getAndSet(null);
        if (ws != null) ws.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
    }

    private class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                handleText(buffer.toString());
                buffer.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("Finnhub WebSocket error: {}", error.getMessage());
            socket.set(null);
            // D25 Circuit Breaker 적용 시 자동 재연결 정교화
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.info("Finnhub WebSocket closed: {} {}", statusCode, reason);
            socket.set(null);
            return CompletableFuture.completedFuture(null);
        }
    }

    /** 패키지 가시성 — 테스트가 핸들러 로직만 직접 검증 가능 */
    void handleText(String raw) {
        for (Quote q : parser.parse(raw)) {
            priceCache.put(q);
            eventPublisher.publishEvent(new PriceUpdatedEvent(q));
        }
    }
}
