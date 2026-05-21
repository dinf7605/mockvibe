package com.fintech.simulator.market.provider.kis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 부팅 시 KIS 인증 + 한국 종목 자동 구독.
 *
 * - OAuth access_token / approval_key 발급 시도 → 키 정합성 검증
 * - 한국 시간 09:00~15:30 외에는 trade 메시지 없음 (연결만 검증)
 * - 토큰/approval_key 발급 실패 시 부팅은 막지 않음 (Circuit Breaker fallback이 stale 캐시 또는 EXTERNAL_API_ERROR 처리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisBootHealthCheck implements ApplicationRunner {

    private static final List<String> SEED_TICKERS = List.of("005930", "000660", "035420");

    private final KisAuthService authService;
    private final KisApprovalKeyService approvalKeyService;
    private final KisWebSocketClient webSocketClient;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String token = authService.getAccessToken();
            log.info("✅ KIS OAuth access_token OK (length={})", token.length());
        } catch (Exception e) {
            log.warn("⚠️ KIS OAuth 실패: {}", e.getMessage());
            return;
        }
        try {
            String approval = approvalKeyService.getApprovalKey();
            log.info("✅ KIS approval_key OK (length={})", approval.length());
        } catch (Exception e) {
            log.warn("⚠️ KIS approval_key 실패: {}", e.getMessage());
            return;
        }
        try {
            SEED_TICKERS.forEach(webSocketClient::subscribe);
            log.info("✅ KIS WebSocket seed subscriptions: {}", SEED_TICKERS);
        } catch (Exception e) {
            log.warn("⚠️ KIS WebSocket subscribe 실패: {}", e.getMessage());
        }
    }
}
