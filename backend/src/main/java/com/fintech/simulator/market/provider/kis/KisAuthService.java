package com.fintech.simulator.market.provider.kis;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * KIS OAuth 2.0 토큰 발급 + 2단 캐싱.
 *
 * <h3>왜 2단 캐싱인가 (KIS "1일 1회 발급" 원칙)</h3>
 * KIS 는 접근토큰을 24h 재사용하도록 권고하며 잦은 재발급을 제한한다.
 * 인메모리 캐시만 두면 재배포/재시작마다 토큰이 사라져 매 부팅 시 재발급되어 원칙에 어긋난다.
 * → <b>Redis(L2)</b> 에 토큰을 TTL 과 함께 저장해 재시작에도 살아남게 하고,
 *   인메모리(L1) 는 매 호출 Redis 왕복을 줄이는 빠른 캐시로 둔다.
 *
 * <ul>
 *   <li>L1 (volatile 인메모리): 유효하면 즉시 반환</li>
 *   <li>L2 (Redis {@code kis:auth:access-token}): L1 미스 시 조회 — 재시작 직후 재사용</li>
 *   <li>둘 다 미스/만료 시에만 KIS 에 신규 발급 후 L1+L2 갱신</li>
 * </ul>
 * 만료 60초 전 갱신. 동시 호출 안전(synchronized). Redis 장애 시 발급으로 폴백.
 * app.external.kis.app-key 가 비어있으면 Bean 비활성.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisAuthService {

    private static final long REFRESH_LEEWAY_SECONDS = 60L;
    private static final String TOKEN_PATH = "/oauth2/tokenP";
    private static final String REDIS_KEY = "kis:auth:access-token";

    private final KisProperties props;
    private final RestClient restClient;
    private final StringRedisTemplate redis;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public KisAuthService(KisProperties props, StringRedisTemplate redis) {
        this.props = props;
        this.redis = redis;
        this.restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    @CircuitBreaker(name = "kis-auth", fallbackMethod = "getAccessTokenFallback")
    public synchronized String getAccessToken() {
        // L1: 인메모리
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(REFRESH_LEEWAY_SECONDS))) {
            return cachedToken;
        }
        // L2: Redis (재시작 직후 재발급 방지)
        String fromRedis = readFromRedis();
        if (fromRedis != null) return fromRedis;

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
            long ttl = res.expiresInSeconds() != null ? res.expiresInSeconds() : 86400L;
            this.cachedToken = res.accessToken();
            this.expiresAt = Instant.now().plusSeconds(ttl);
            writeToRedis(res.accessToken(), ttl);
            log.info("KIS access token newly issued, expiresAt={}", expiresAt);
            return cachedToken;
        } catch (RestClientException e) {
            log.warn("KIS token request failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "KIS 인증 실패");
        }
    }

    /** L2(Redis)에서 유효 토큰 조회 → 있으면 L1 채우고 반환. 없거나 Redis 장애면 null. */
    private String readFromRedis() {
        try {
            String token = redis.opsForValue().get(REDIS_KEY);
            if (token == null) return null;
            Long ttl = redis.getExpire(REDIS_KEY);  // 초
            if (ttl == null || ttl <= REFRESH_LEEWAY_SECONDS) return null;
            this.cachedToken = token;
            this.expiresAt = Instant.now().plusSeconds(ttl);
            log.info("KIS access token reused from Redis (ttl={}s) — 재발급 회피", ttl);
            return token;
        } catch (Exception e) {
            log.warn("KIS token Redis 조회 실패 — 신규 발급으로 진행: {}", e.toString());
            return null;
        }
    }

    private void writeToRedis(String token, long ttlSeconds) {
        try {
            redis.opsForValue().set(REDIS_KEY, token, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("KIS token Redis 저장 실패(무시): {}", e.toString());
        }
    }

    /** 테스트/관리자용 강제 무효화 (L1+L2). */
    public synchronized void invalidate() {
        this.cachedToken = null;
        this.expiresAt = Instant.EPOCH;
        try { redis.delete(REDIS_KEY); } catch (Exception ignored) { /* best-effort */ }
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
