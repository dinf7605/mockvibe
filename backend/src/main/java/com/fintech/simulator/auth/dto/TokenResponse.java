package com.fintech.simulator.auth.dto;

import com.fintech.simulator.auth.domain.Role;

/**
 * 로그인/재발급 응답.
 * Refresh Token은 응답 본문이 아니라 httpOnly 쿠키로 전달되므로 포함하지 않는다.
 */
public record TokenResponse(
        String accessToken,
        long accessTokenExpiresInSeconds,
        String userId,
        String username,
        Role role
) {}
