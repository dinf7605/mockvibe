package com.fintech.simulator.auth.jwt;

import com.fintech.simulator.auth.domain.Role;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 발급/검증.
 * - 알고리즘: HS256 (대칭키)
 * - 클레임: sub=userId, role, type=ACCESS|REFRESH, jti(블랙리스트/Rotation 추적용)
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes (256 bits) for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(String userId, Role role) {
        return build(userId, role, TokenType.ACCESS, props.accessTokenValiditySeconds());
    }

    public String createRefreshToken(String userId, Role role) {
        return build(userId, role, TokenType.REFRESH, props.refreshTokenValiditySeconds());
    }

    private String build(String userId, Role role, TokenType type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, type.name())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 토큰 파싱·서명·만료 검증. 실패 시 BusinessException 던짐.
     */
    public TokenClaims parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            Claims c = jws.getPayload();
            return new TokenClaims(
                    c.getSubject(),
                    Role.valueOf(c.get(CLAIM_ROLE, String.class)),
                    TokenType.valueOf(c.get(CLAIM_TYPE, String.class)),
                    c.getId(),
                    c.getExpiration().toInstant().getEpochSecond()
            );
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    public long accessTokenTtlSeconds()  { return props.accessTokenValiditySeconds(); }
    public long refreshTokenTtlSeconds() { return props.refreshTokenValiditySeconds(); }
}
