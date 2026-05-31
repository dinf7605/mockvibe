package com.fintech.simulator.market.provider.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * KIS WebSocket 인증용 approval_key 발급 + 2단 캐싱.
 *
 * - 엔드포인트: POST /oauth2/Approval (access_token과는 별개, 발급 제한도 별개)
 * - 유효기간 24h, 만료 60초 전 갱신
 * - access token 과 동일하게 <b>Redis(L2)</b> 에 저장해 재시작에도 재사용 → 잦은 재발급 회피.
 *   인메모리(L1) 는 빠른 캐시. Redis 장애 시 발급으로 폴백.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisApprovalKeyService {

    private static final long REFRESH_LEEWAY_SECONDS = 60L;
    private static final long DEFAULT_TTL_SECONDS = 86400L;
    private static final String APPROVAL_PATH = "/oauth2/Approval";
    private static final String REDIS_KEY = "kis:auth:approval-key";

    private final KisProperties props;
    private final RestClient restClient;
    private final StringRedisTemplate redis;

    private volatile String cachedKey;
    private volatile Instant expiresAt = Instant.EPOCH;

    public KisApprovalKeyService(KisProperties props, StringRedisTemplate redis) {
        this.props = props;
        this.redis = redis;
        this.restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    @CircuitBreaker(name = "kis-approval", fallbackMethod = "getApprovalKeyFallback")
    public synchronized String getApprovalKey() {
        if (cachedKey != null && Instant.now().isBefore(expiresAt.minusSeconds(REFRESH_LEEWAY_SECONDS))) {
            return cachedKey;
        }
        String fromRedis = readFromRedis();
        if (fromRedis != null) return fromRedis;
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
            writeToRedis(res.approvalKey(), DEFAULT_TTL_SECONDS);
            log.info("KIS approval_key newly issued, expiresAt={}", expiresAt);
            return cachedKey;
        } catch (RestClientException e) {
            log.warn("KIS approval_key request failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "KIS WebSocket 인증 실패");
        }
    }

    private String readFromRedis() {
        try {
            String key = redis.opsForValue().get(REDIS_KEY);
            if (key == null) return null;
            Long ttl = redis.getExpire(REDIS_KEY);
            if (ttl == null || ttl <= REFRESH_LEEWAY_SECONDS) return null;
            this.cachedKey = key;
            this.expiresAt = Instant.now().plusSeconds(ttl);
            log.info("KIS approval_key reused from Redis (ttl={}s) — 재발급 회피", ttl);
            return key;
        } catch (Exception e) {
            log.warn("KIS approval_key Redis 조회 실패 — 신규 발급으로 진행: {}", e.toString());
            return null;
        }
    }

    private void writeToRedis(String key, long ttlSeconds) {
        try {
            redis.opsForValue().set(REDIS_KEY, key, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("KIS approval_key Redis 저장 실패(무시): {}", e.toString());
        }
    }

    public synchronized void invalidate() {
        this.cachedKey = null;
        this.expiresAt = Instant.EPOCH;
        try { redis.delete(REDIS_KEY); } catch (Exception ignored) { /* best-effort */ }
    }

    @SuppressWarnings("unused")
    public String getApprovalKeyFallback(Throwable t) {
        log.warn("KIS approval CB fallback: {}", t.getMessage());
        if (cachedKey != null) return cachedKey;
        throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "KIS WebSocket 인증 회로 차단");
    }

    public record ApprovalResponse(@JsonProperty("approval_key") String approvalKey) {}
}
