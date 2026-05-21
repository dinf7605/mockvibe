package com.fintech.simulator.auth.jwt;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Bearer Access Token 인증 필터.
 * 토큰 검증 + 블랙리스트 체크 후 SecurityContext에 인증 객체 설정.
 * 토큰이 없거나 유효하지 않으면 그대로 다음 필터로 흘려보낸다 (entryPoint가 401 처리).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwt;
    private final AccessTokenBlacklist blacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(req);
        if (token != null) {
            try {
                TokenClaims claims = jwt.parse(token);
                if (claims.type() != TokenType.ACCESS) {
                    throw new BusinessException(ErrorCode.INVALID_TOKEN);
                }
                if (blacklist.isRevoked(claims.jti())) {
                    throw new BusinessException(ErrorCode.TOKEN_REVOKED);
                }

                AbstractAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        claims.userId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()))
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (BusinessException e) {
                log.debug("JWT auth failed: {}", e.getErrorCode());
                // SecurityContext는 비워둔다 → EntryPoint가 401 응답 생성
                SecurityContextHolder.clearContext();
                req.setAttribute("jwtError", e.getErrorCode());
            }
        }
        chain.doFilter(req, res);
    }

    private String resolveToken(HttpServletRequest req) {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
