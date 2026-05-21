package com.fintech.simulator.auth.controller;

import com.fintech.simulator.auth.dto.LoginRequest;
import com.fintech.simulator.auth.dto.SignupRequest;
import com.fintech.simulator.auth.dto.SignupResponse;
import com.fintech.simulator.auth.dto.TokenResponse;
import com.fintech.simulator.auth.jwt.JwtTokenProvider;
import com.fintech.simulator.auth.jwt.RefreshCookieFactory;
import com.fintech.simulator.auth.jwt.TokenClaims;
import com.fintech.simulator.auth.service.AuthService;
import com.fintech.simulator.auth.service.AuthService.AuthResult;
import com.fintech.simulator.auth.service.SignupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SignupService signupService;
    private final AuthService authService;
    private final RefreshCookieFactory cookieFactory;
    private final JwtTokenProvider jwt;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return signupService.signup(request);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult r = authService.login(request);
        return tokenResponse(r);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "${app.cookie.refresh-token-name}", required = false) String refreshToken
    ) {
        AuthResult r = authService.refresh(refreshToken);
        return tokenResponse(r);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(
            HttpServletRequest httpReq,
            @CookieValue(name = "${app.cookie.refresh-token-name}", required = false) String refreshToken
    ) {
        TokenClaims accessClaims = null;
        String authHeader = httpReq.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            try {
                accessClaims = jwt.parse(authHeader.substring(BEARER_PREFIX.length()));
            } catch (Exception ignored) {
                // 이미 만료/위조면 무시 — 로그아웃은 멱등적으로 수행
            }
        }

        String userId = accessClaims != null ? accessClaims.userId() : null;
        if (userId == null && refreshToken != null) {
            try {
                userId = jwt.parse(refreshToken).userId();
            } catch (Exception ignored) {}
        }
        if (userId != null) {
            authService.logout(userId, accessClaims);
        }

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expire().toString())
                .build();
    }

    private ResponseEntity<TokenResponse> tokenResponse(AuthResult r) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieFactory.issue(r.refreshToken(), jwt.refreshTokenTtlSeconds()).toString())
                .body(r.body());
    }
}
