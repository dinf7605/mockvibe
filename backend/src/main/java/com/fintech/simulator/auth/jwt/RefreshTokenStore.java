package com.fintech.simulator.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Refresh Token Redis 저장소.
 * 키: RT:{userId} → token 문자열, TTL = refresh validity
 * Rotation: save 시 기존 값을 무조건 덮어쓴다 (이전 RT 자동 무효화).
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "RT:";

    private final StringRedisTemplate redis;

    public void save(String userId, String token, long ttlSeconds) {
        redis.opsForValue().set(key(userId), token, Duration.ofSeconds(ttlSeconds));
    }

    public Optional<String> find(String userId) {
        return Optional.ofNullable(redis.opsForValue().get(key(userId)));
    }

    public void delete(String userId) {
        redis.delete(key(userId));
    }

    public boolean matches(String userId, String token) {
        return find(userId).map(t -> t.equals(token)).orElse(false);
    }

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }
}
