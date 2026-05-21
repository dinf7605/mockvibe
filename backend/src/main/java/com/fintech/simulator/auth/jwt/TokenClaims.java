package com.fintech.simulator.auth.jwt;

import com.fintech.simulator.auth.domain.Role;

/**
 * 파싱된 JWT의 핵심 정보. 인증 필터와 서비스 간 전달용 DTO.
 */
public record TokenClaims(
        String userId,
        Role role,
        TokenType type,
        String jti,
        long expiresAtEpochSec
) {}
