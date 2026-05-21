package com.fintech.simulator.auth.service;

import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.domain.UserStatus;
import com.fintech.simulator.auth.dto.LoginRequest;
import com.fintech.simulator.auth.dto.TokenResponse;
import com.fintech.simulator.auth.jwt.AccessTokenBlacklist;
import com.fintech.simulator.auth.jwt.JwtTokenProvider;
import com.fintech.simulator.auth.jwt.RefreshTokenStore;
import com.fintech.simulator.auth.jwt.TokenClaims;
import com.fintech.simulator.auth.jwt.TokenType;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 / 토큰 재발급 / 로그아웃.
 *
 * - 로그인: BCrypt 검증 → AT 발급 + RT 발급 → Redis(RT:userId)에 RT 저장 → 응답
 * - 재발급(Rotation): 쿠키 RT 검증 → Redis RT 비교 → 새 AT+RT 발급 → Redis 교체
 * - 로그아웃: Redis RT 삭제 + AT의 jti를 블랙리스트에 등록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final RefreshTokenStore refreshTokenStore;
    private final AccessTokenBlacklist blacklist;

    public record AuthResult(TokenResponse body, String refreshToken) {}

    @Transactional
    public AuthResult login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        user.markLoggedIn();
        return issueTokens(user);
    }

    /**
     * Refresh Token Rotation.
     * 쿠키로 받은 RT를 검증한 뒤, Redis에 보관된 값과 정확히 일치할 때만 재발급한다.
     * 일치하지 않으면 토큰 탈취 가능성이 있으므로 Redis RT를 삭제(강제 로그아웃).
     */
    @Transactional
    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING);
        }
        TokenClaims claims = jwt.parse(refreshToken);
        if (claims.type() != TokenType.REFRESH) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if (!refreshTokenStore.matches(claims.userId(), refreshToken)) {
            // 재사용 또는 위조 감지 — 즉시 모든 RT 무효화
            refreshTokenStore.delete(claims.userId());
            throw new BusinessException(ErrorCode.TOKEN_REVOKED);
        }

        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            refreshTokenStore.delete(user.getUserId());
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        return issueTokens(user);
    }

    /**
     * 로그아웃.
     * - Redis의 RT 삭제 → 이후 재발급 불가
     * - 현재 AT의 jti를 블랙리스트에 등록 → 만료 전까지 SecurityFilter가 거부
     */
    public void logout(String userId, TokenClaims accessClaims) {
        refreshTokenStore.delete(userId);
        if (accessClaims != null) {
            blacklist.revoke(accessClaims.jti(), accessClaims.expiresAtEpochSec());
        }
        log.info("Logout: userId={}", userId);
    }

    private AuthResult issueTokens(User user) {
        String accessToken  = jwt.createAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwt.createRefreshToken(user.getUserId(), user.getRole());
        refreshTokenStore.save(user.getUserId(), refreshToken, jwt.refreshTokenTtlSeconds());

        TokenResponse body = new TokenResponse(
                accessToken,
                jwt.accessTokenTtlSeconds(),
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
        return new AuthResult(body, refreshToken);
    }
}
