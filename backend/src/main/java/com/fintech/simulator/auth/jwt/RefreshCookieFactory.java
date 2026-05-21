package com.fintech.simulator.auth.jwt;

import com.fintech.simulator.config.CookieProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 쿠키 생성 헬퍼.
 * - httpOnly, path=/, SameSite·secure는 환경별 CookieProperties로 제어
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    private final CookieProperties props;

    public ResponseCookie issue(String token, long ttlSeconds) {
        return baseBuilder(token)
                .maxAge(Duration.ofSeconds(ttlSeconds))
                .build();
    }

    /** 로그아웃 시 즉시 만료 쿠키 (Max-Age=0) */
    public ResponseCookie expire() {
        return baseBuilder("").maxAge(Duration.ZERO).build();
    }

    public String cookieName() {
        return props.refreshTokenName();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
        return ResponseCookie.from(props.refreshTokenName(), value)
                .httpOnly(true)
                .secure(props.secure())
                .sameSite(props.sameSite())
                .path("/");
    }
}
