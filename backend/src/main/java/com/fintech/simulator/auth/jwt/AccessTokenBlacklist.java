package com.fintech.simulator.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Access Token 블랙리스트.
 * 로그아웃 시 jti(JWT ID)를 등록하고, TTL은 토큰 남은 만료시간으로 설정.
 * 토큰 만료가 지나면 Redis가 자동으로 제거하므로 무한히 자라지 않는다.
 */
@Component
@RequiredArgsConstructor
public class AccessTokenBlacklist {

    private static final String KEY_PREFIX = "BL:AT:";
    private static final String VALUE = "1";

    private final StringRedisTemplate redis;

    public void revoke(String jti, long expiresAtEpochSec) {
        long remainSec = expiresAtEpochSec - Instant.now().getEpochSecond();
        if (remainSec <= 0) {
            return; // 이미 만료 — 블랙리스트 불필요
        }
        redis.opsForValue().set(key(jti), VALUE, Duration.ofSeconds(remainSec));
    }

    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(key(jti)));
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }
}
