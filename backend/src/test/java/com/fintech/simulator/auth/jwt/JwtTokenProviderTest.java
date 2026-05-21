package com.fintech.simulator.auth.jwt;

import com.fintech.simulator.auth.domain.Role;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-xxx";
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(new JwtProperties(900, 604800, "fintech-simulator", SECRET));
    }

    @Test
    @DisplayName("AccessToken을 생성하면 sub/role/type/jti가 파싱된다")
    void access_token_round_trip() {
        String token = provider.createAccessToken("user-123", Role.USER);
        TokenClaims c = provider.parse(token);

        assertThat(c.userId()).isEqualTo("user-123");
        assertThat(c.role()).isEqualTo(Role.USER);
        assertThat(c.type()).isEqualTo(TokenType.ACCESS);
        assertThat(c.jti()).isNotBlank();
        assertThat(c.expiresAtEpochSec()).isPositive();
    }

    @Test
    @DisplayName("RefreshToken은 type=REFRESH")
    void refresh_token_type() {
        String token = provider.createRefreshToken("user-123", Role.ADMIN);
        TokenClaims c = provider.parse(token);

        assertThat(c.type()).isEqualTo(TokenType.REFRESH);
        assertThat(c.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("위조된 토큰이면 INVALID_TOKEN 예외")
    void tampered_token() {
        String token = provider.createAccessToken("u", Role.USER);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> provider.parse(tampered))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("이미 만료된 토큰이면 TOKEN_EXPIRED 예외")
    void expired_token() throws Exception {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                new JwtProperties(-1, -1, "fintech-simulator", SECRET));
        String token = shortLived.createAccessToken("u", Role.USER);

        assertThatThrownBy(() -> shortLived.parse(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
        // 참조 방지 경고 제거용
        unused(Field.class);
    }

    @Test
    @DisplayName("32바이트 미만 secret은 IllegalState로 거부")
    void short_secret_rejected() {
        assertThatThrownBy(() ->
                new JwtTokenProvider(new JwtProperties(900, 604800, "iss", "tooShort")))
                .isInstanceOf(IllegalStateException.class);
    }

    @SuppressWarnings("unused")
    private void unused(Object o) {}
}
