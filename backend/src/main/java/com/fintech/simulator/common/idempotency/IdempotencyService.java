package com.fintech.simulator.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 멱등 처리 — 동일 (userId, Idempotency-Key) 요청을 한 번만 실행하고 결과를 재사용한다.
 *
 * <h3>왜 필요한가</h3>
 * 매수/매도 같은 비멱등 POST 는 네트워크 재시도·더블클릭·새로고침으로 중복 실행되면
 * 잔고가 두 번 차감된다. 클라이언트가 보낸 Idempotency-Key 를 Redis 에 기록해
 * 같은 키의 재요청에는 저장된 응답을 그대로 돌려준다 (fintech 표준 패턴).
 *
 * <h3>키/TTL</h3>
 * {@code idem:{userId}:{key}} 에 응답 JSON 저장, TTL 24h.
 * userId 를 키에 포함해 다른 사용자의 키와 충돌하지 않게 한다.
 *
 * <h3>한계 (포트폴리오 범위)</h3>
 * "조회 후 처리 → 저장" 사이의 매우 좁은 경쟁 구간에서 동시 중복 요청이 둘 다 실행될 수
 * 있다. 완전 차단하려면 setIfAbsent 선점 락 + 진행중 상태가 필요하나, 일반적인 재시도·
 * 더블클릭 방지에는 본 구현으로 충분하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PREFIX = "idem:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * key 가 있으면 멱등 처리, 없으면 그냥 action 실행.
     *
     * @param userId 멱등 범위(사용자별)
     * @param key    Idempotency-Key 헤더 값 (null/blank 면 멱등 처리 생략)
     * @param type   응답 역직렬화 타입
     * @param action 실제 처리 (캐시 미스 시 1회 실행)
     */
    public <T> T execute(String userId, String key, Class<T> type, Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        String redisKey = PREFIX + userId + ":" + key;

        String cached = redis.opsForValue().get(redisKey);
        if (cached != null) {
            try {
                T replay = objectMapper.readValue(cached, type);
                log.debug("Idempotent replay: {}", redisKey);
                return replay;
            } catch (Exception e) {
                log.warn("Idempotency cache 역직렬화 실패 — 재처리: {} ({})", redisKey, e.toString());
            }
        }

        T result = action.get();
        try {
            redis.opsForValue().set(redisKey, objectMapper.writeValueAsString(result), TTL);
        } catch (Exception e) {
            // 캐시 저장 실패는 무시 — 주문 자체는 이미 성공했다.
            log.warn("Idempotency cache 저장 실패: {} ({})", redisKey, e.toString());
        }
        return result;
    }
}
