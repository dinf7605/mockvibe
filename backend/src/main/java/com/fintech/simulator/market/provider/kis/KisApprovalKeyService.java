package com.fintech.simulator.market.provider.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;

/**
 * KIS WebSocket 인증용 approval_key 발급.
 *
 * - 엔드포인트: POST /oauth2/Approval (access_token과는 별개)
 * - 유효기간: 24h (만료 60초 전 갱신)
 * - 매 WebSocket 연결의 헤더 approval_key로 사용
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisApprovalKeyService {

    private static final long REFRESH_LEEWAY_SECONDS = 60L;
    private static final long DEFAULT_TTL_SECONDS = 86400L;
    private static final String APPROVAL_PATH = "/oauth2/Approval";

    private final KisProperties props;
    private final RestClient restClient;

    private volatile String cachedKey;
    private volatile Instant expiresAt = Instant.EPOCH;

    public KisApprovalKeyService(KisProperties props) {
        this.props = props;
        this.restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    @CircuitBreaker(name = "kis-approval", fallbackMethod = "getApprovalKeyFallback")
    public synchronized String getApprovalKey() {
        if (cachedKey != null && Instant.now().isBefore(expiresAt.minusSeconds(REFRESH_LEEWAY_SECONDS))) {
            return cachedKey;
        }
        try {
            ApprovalResponse res = restClient.post()
                    .uri(APPROVAL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "grant_type", "client_credentials",
                            "appkey",     props.appKey(),
                            "secretkey",  props.appSecret()  // KIS Approval은 secretkey 키명 사용
                    ))
                    .retrieve()
                    .body(ApprovalResponse.class);

            if (res == null || res.approvalKey() == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "KIS approval_key 응답이 비어 있습니다.");
            }
            this.cachedKey = res.approvalKey();
            this.expiresAt = Instant.now().plusSeconds(DEFAULT_TTL_SECONDS);
            log.info("KIS approval_key refreshed, expiresAt={}", expiresAt);
            return cachedKey;
        } catch (RestClientException e) {
            log.warn("KIS approval_key request failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "KIS WebSocket 인증 실패");
        }
    }

    public synchronized void invalidate() {
        this.cachedKey = null;
        this.expiresAt = Instant.EPOCH;
    }

    @SuppressWarnings("unused")
    public String getApprovalKeyFallback(Throwable t) {
        log.warn("KIS approval CB fallback: {}", t.getMessage());
        if (cachedKey != null) return cachedKey;
        throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "KIS WebSocket 인증 회로 차단");
    }

    public record ApprovalResponse(@JsonProperty("approval_key") String approvalKey) {}
}
