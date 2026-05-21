package com.fintech.simulator.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 `app.jwt.*` 매핑.
 * - secret: HS256용 비밀키 (운영은 env JWT_SECRET 주입, 최소 32바이트)
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        long accessTokenValiditySeconds,
        long refreshTokenValiditySeconds,
        String issuer,
        String secret
) {}
