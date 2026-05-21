package com.fintech.simulator.market.provider.kis;

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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

/**
 * KIS WebSocket 클라이언트 (실시간 체결 H0STCNT0 구독).
 *
 *  - 연결: ws://ops.koreainvestment.com:31000 (모의)
 *  - 인증: 첫 구독 메시지 헤더의 approval_key
 *  - 메시지 처리: KisQuoteMessageParser → PriceCache + PriceUpdatedEvent
 *  - 재연결: onClose/onError → Exponential Backoff (PRD FR-5.3)
 *
 * D13 골격. 실 운영 시 KIS 키 발급 후 재연결·heartbeat 정교화.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisWebSocketClient {

    private static final String STOCK_EXEC_TR_ID = "H0STCNT0";

    private final KisProperties props;
    private final KisApprovalKeyService approvalKeyService;
    private final KisSubscriptionManager subscriptions;
    private final PriceCache priceCache;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private volatile boolean shuttingDown = false;

    /** 종목 구독. 미연결이면 먼저 connect. */
    public synchronized void subscribe(String ticker) {
        subscriptions.subscribe(ticker);
        ensureConnected();
        sendSubscribeMessage(ticker, "1"); // tr_type=1: 등록
    }

    public synchronized void unsubscribe(String ticker) {
        if (subscriptions.unsubscribe(ticker)) {
            sendSubscribeMessage(ticker, "2"); // tr_type=2: 해제
        }
    }

    private void ensureConnected() {
        if (socket.get() != null) return;
        try {
            WebSocket ws = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(props.wsUrl()), new Listener())
                    .join();
            socket.set(ws);
            log.info("KIS WebSocket connected: {}", props.wsUrl());
        } catch (Exception e) {
            log.warn("KIS WebSocket connect failed: {}", e.getMessage());
        }
    }

    private void sendSubscribeMessage(String ticker, String trType) {
        WebSocket ws = socket.get();
        if (ws == null) return;
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "header", Map.of(
                            "approval_key", approvalKeyService.getApprovalKey(),
                            "custtype", "P",
                            "tr_type", trType,
                            "content-type", "utf-8"),
                    "body", Map.of(
                            "input", Map.of("tr_id", STOCK_EXEC_TR_ID, "tr_key", ticker))
            ));
            ws.sendText(payload, true);
        } catch (JsonProcessingException e) {
            log.warn("KIS subscribe payload build failed: {}", e.getMessage());
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        shuttingDown = true;
        WebSocket ws = socket.getAndSet(null);
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }

    /** Listener는 별도 객체로 분리해서 단위 테스트 가능하도록 onMessage 로직만 노출 */
    private class Listener implements WebSocket.Listener {

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            handleText(data.toString());
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("KIS WebSocket error: {}", error.getMessage());
            socket.set(null);
            if (!shuttingDown) {
                // D14에서 Exponential Backoff 재연결 도입 (Finnhub와 공통화)
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.info("KIS WebSocket closed: {} {}", statusCode, reason);
            socket.set(null);
            return CompletableFuture.completedFuture(null);
        }
    }

    /** 패키지 가시성 — 테스트가 메시지 처리 로직만 직접 호출할 수 있게 분리 */
    void handleText(String raw) {
        Optional<Quote> q = KisQuoteMessageParser.parse(raw);
        if (q.isEmpty()) return;
        Quote quote = q.get();
        priceCache.put(quote);
        eventPublisher.publishEvent(new PriceUpdatedEvent(quote));
    }
}
