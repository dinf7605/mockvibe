package com.fintech.simulator.auth.service;

import com.fintech.simulator.auth.domain.Role;
import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.dto.LoginRequest;
import com.fintech.simulator.auth.jwt.AccessTokenBlacklist;
import com.fintech.simulator.auth.jwt.JwtProperties;
import com.fintech.simulator.auth.jwt.JwtTokenProvider;
import com.fintech.simulator.auth.jwt.RefreshTokenStore;
import com.fintech.simulator.auth.jwt.TokenClaims;
import com.fintech.simulator.auth.jwt.TokenType;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.auth.service.AuthService.AuthResult;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenStore refreshTokenStore;
    @Mock AccessTokenBlacklist blacklist;

    JwtTokenProvider jwt;
    AuthService service;

    @BeforeEach
    void setUp() {
        jwt = new JwtTokenProvider(new JwtProperties(900, 604800, "iss",
                "test-secret-key-must-be-at-least-32-bytes-xxx"));
        service = new AuthService(userRepository, passwordEncoder, jwt, refreshTokenStore, blacklist);
    }

    @Test
    @DisplayName("login: 비밀번호 일치 시 AT/RT 발급 + Redis에 RT 저장")
    void login_success() {
        User user = User.newUser("홍길동", "hong@example.com", "encoded");
        given(userRepository.findByEmail("hong@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("plain", "encoded")).willReturn(true);

        AuthResult r = service.login(new LoginRequest("hong@example.com", "plain"));

        assertThat(r.body().accessToken()).isNotBlank();
        assertThat(r.refreshToken()).isNotBlank();
        assertThat(r.body().userId()).isEqualTo(user.getUserId());
        assertThat(jwt.parse(r.body().accessToken()).type()).isEqualTo(TokenType.ACCESS);
        assertThat(jwt.parse(r.refreshToken()).type()).isEqualTo(TokenType.REFRESH);

        verify(refreshTokenStore, times(1)).save(eq(user.getUserId()), eq(r.refreshToken()), anyLong());
    }

    @Test
    @DisplayName("login: 비밀번호 불일치 시 INVALID_CREDENTIALS")
    void login_invalid_password() {
        User user = User.newUser("홍길동", "hong@example.com", "encoded");
        given(userRepository.findByEmail("hong@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("hong@example.com", "wrong")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(refreshTokenStore, never()).save(any(), any(), anyLong());
    }

    @Test
    @DisplayName("login: 존재하지 않는 이메일 시 INVALID_CREDENTIALS (사용자 존재 여부 노출 방지)")
    void login_unknown_email() {
        given(userRepository.findByEmail("nope@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nope@example.com", "x")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("login: 정지된 계정이면 ACCOUNT_SUSPENDED")
    void login_suspended() {
        User user = User.newUser("홍길동", "hong@example.com", "encoded");
        user.suspend();
        given(userRepository.findByEmail("hong@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("plain", "encoded")).willReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("hong@example.com", "plain")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_SUSPENDED);
    }

    @Test
    @DisplayName("refresh: Redis RT와 일치할 때만 새 AT/RT 발급 (Rotation)")
    void refresh_rotation() {
        User user = User.newUser("홍길동", "hong@example.com", "encoded");
        String rt = jwt.createRefreshToken(user.getUserId(), user.getRole());
        given(refreshTokenStore.matches(user.getUserId(), rt)).willReturn(true);
        given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));

        AuthResult r = service.refresh(rt);

        assertThat(r.refreshToken()).isNotBlank().isNotEqualTo(rt);
        verify(refreshTokenStore, times(1)).save(eq(user.getUserId()), eq(r.refreshToken()), anyLong());
    }

    @Test
    @DisplayName("refresh: 쿠키 누락 시 REFRESH_TOKEN_MISSING")
    void refresh_missing() {
        assertThatThrownBy(() -> service.refresh(null))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("refresh: AccessToken을 RT로 보내면 INVALID_TOKEN")
    void refresh_wrong_type() {
        String at = jwt.createAccessToken("u", Role.USER);

        assertThatThrownBy(() -> service.refresh(at))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("refresh: Redis 값과 다르면 TOKEN_REVOKED + Redis 강제 삭제")
    void refresh_mismatch_kicks_out() {
        User user = User.newUser("홍길동", "hong@example.com", "encoded");
        String rt = jwt.createRefreshToken(user.getUserId(), user.getRole());
        given(refreshTokenStore.matches(user.getUserId(), rt)).willReturn(false);

        assertThatThrownBy(() -> service.refresh(rt))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_REVOKED);
        verify(refreshTokenStore, times(1)).delete(user.getUserId());
    }

    @Test
    @DisplayName("logout: RT 삭제 + AT jti를 블랙리스트에 추가")
    void logout_revokes() {
        User user = User.newUser("홍길동", "hong@example.com", "encoded");
        String at = jwt.createAccessToken(user.getUserId(), user.getRole());
        TokenClaims claims = jwt.parse(at);

        service.logout(user.getUserId(), claims);

        verify(refreshTokenStore, times(1)).delete(user.getUserId());
        verify(blacklist, times(1)).revoke(claims.jti(), claims.expiresAtEpochSec());
    }

    // 짧은 헬퍼
    private static <T> T eq(T v) { return org.mockito.ArgumentMatchers.eq(v); }
}
