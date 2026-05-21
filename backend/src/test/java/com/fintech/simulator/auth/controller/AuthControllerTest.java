package com.fintech.simulator.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.simulator.auth.domain.Role;
import com.fintech.simulator.auth.dto.LoginRequest;
import com.fintech.simulator.auth.dto.SignupRequest;
import com.fintech.simulator.auth.dto.SignupResponse;
import com.fintech.simulator.auth.dto.TokenResponse;
import com.fintech.simulator.auth.jwt.JwtAuthenticationEntryPoint;
import com.fintech.simulator.auth.jwt.JwtAuthenticationFilter;
import com.fintech.simulator.auth.jwt.JwtTokenProvider;
import com.fintech.simulator.auth.jwt.RefreshCookieFactory;
import com.fintech.simulator.auth.service.AuthService;
import com.fintech.simulator.auth.service.AuthService.AuthResult;
import com.fintech.simulator.auth.service.SignupService;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "app.cookie.refresh-token-name=refresh_token")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockitoBean SignupService signupService;
    @MockitoBean AuthService authService;
    @MockitoBean RefreshCookieFactory cookieFactory;
    @MockitoBean JwtTokenProvider jwt;
    // @WebMvcTest가 Filter 빈을 자동 스캔하므로 그 의존성까지 채우기 위해 함께 mock
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private TokenResponse sampleToken() {
        return new TokenResponse("AT.xxx", 900, "uuid-123", "홍길동", Role.USER);
    }

    // ===== signup =====

    @Test
    @DisplayName("POST /auth/signup — 정상 요청 시 201")
    void signup_201() throws Exception {
        SignupRequest req = new SignupRequest("홍길동", "hong@example.com", "Passw0rd!");
        SignupResponse res = new SignupResponse(
                "uuid-123", "홍길동", "hong@example.com", Role.USER, new BigDecimal("10000000"));
        given(signupService.signup(any())).willReturn(res);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("uuid-123"))
                .andExpect(jsonPath("$.seedMoneyKrw").value(10000000));
    }

    @Test
    @DisplayName("회원가입 이메일 형식 위반 시 400")
    void signup_validation() throws Exception {
        SignupRequest req = new SignupRequest("홍길동", "not-email", "Passw0rd!");
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    // ===== login =====

    @Test
    @DisplayName("POST /auth/login — 200 + Set-Cookie(refresh_token)")
    void login_200_setCookie() throws Exception {
        LoginRequest req = new LoginRequest("hong@example.com", "Passw0rd!");
        given(authService.login(any())).willReturn(new AuthResult(sampleToken(), "RT.xxx"));
        given(jwt.refreshTokenTtlSeconds()).willReturn(604800L);
        given(cookieFactory.issue(any(), org.mockito.ArgumentMatchers.anyLong()))
                .willReturn(ResponseCookie.from("refresh_token", "RT.xxx")
                        .httpOnly(true).path("/").maxAge(604800).build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("AT.xxx"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    @DisplayName("login 실패(자격 불일치) → 401 INVALID_CREDENTIALS")
    void login_401() throws Exception {
        given(authService.login(any())).willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new LoginRequest("a@b.com", "x"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    // ===== refresh =====

    @Test
    @DisplayName("POST /auth/refresh — 쿠키 누락 → 401 REFRESH_TOKEN_MISSING")
    void refresh_missing_cookie() throws Exception {
        given(authService.refresh(null))
                .willThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING));

        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_006"));
    }
}
