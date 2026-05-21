package com.fintech.simulator.market.provider.kis;

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
 * KIS OAuth 2.0 토큰 발급 + 메모리 캐싱.
 *
 * - 토큰 유효기간 86400초(24h). 만료 60초 전 갱신.
 * - 동시 호출 안전: synchronized 블록.
 * - app.external.kis.app-key가 비어있으면 Bean 비활성 (env 미주입 환경 보호).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisAuthService {

    private static final long REFRESH_LEEWAY_SECONDS = 60L;
    private static final String TOKEN_PATH = "/oauth2/tokenP";

    private final KisProperties props;
    private final RestClient restClient;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public KisAuthService(KisProperties props) {
        this.props = props;
        this.restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    @CircuitBreaker(name = "kis-auth", fallbackMethod = "getAccessTokenFallback")
    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(REFRESH_LEEWAY_SECONDS))) {
            return cachedToken;
        }
        try {
            KisTokenResponse res = restClient.post()
                    .uri(TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "grant_type", "client_credentials",
                            "appkey",     props.appKey(),
                            "appsecret",  props.appSecret()
                    ))
                    .retrieve()
                    .body(KisTokenResponse.class);

            if (res == null || res.accessToken() == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "KIS 토큰 응답이 비어 있습니다.");
            }
            this.cachedToken = res.accessToken();
            long ttl = res.expiresInSeconds() != null ? res.expiresInSeconds() : 86400L;
            this.expiresAt = Instant.now().plusSeconds(ttl);
            log.info("KIS access token refreshed, expiresAt={}", expiresAt);
            return cachedToken;
        } catch (RestClientException e) {
            log.warn("KIS token request failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "KIS 인증 실패");
        }
    }

    /** 테스트/관리자용 강제 무효화 */
    public synchronized void invalidate() {
        this.cachedToken = null;
        this.expiresAt = Instant.EPOCH;
    }

    /**
     * CircuitBreaker Open 또는 호출 실패 시 fallback.
     * 캐시된 토큰이 있으면 stale이라도 반환(매매 기능 유지). 없으면 EXTERNAL_API_ERROR.
     */
    @SuppressWarnings("unused")
    public String getAccessTokenFallback(Throwable t) {
        log.warn("KIS auth CB fallback: {}", t.getMessage());
        if (cachedToken != null) return cachedToken;
        throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "KIS 인증 회로 차단 — 캐시된 토큰도 없습니다.");
    }
}
