package com.fintech.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh Token 쿠키 발급 설정.
 * - secure: HTTPS 필수 여부 (운영 true)
 * - sameSite: Lax | Strict | None
 */
@ConfigurationProperties(prefix = "app.cookie")
public record CookieProperties(
        String refreshTokenName,
        boolean secure,
        String sameSite
) {
    public CookieProperties {
        if (refreshTokenName == null || refreshTokenName.isBlank()) {
            refreshTokenName = "refresh_token";
        }
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Lax";
        }
    }
}
