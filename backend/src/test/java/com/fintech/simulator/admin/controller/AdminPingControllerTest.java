package com.fintech.simulator.admin.controller;

import com.fintech.simulator.auth.jwt.JwtAccessDeniedHandler;
import com.fintech.simulator.auth.jwt.JwtAuthenticationEntryPoint;
import com.fintech.simulator.auth.jwt.JwtAuthenticationFilter;
import com.fintech.simulator.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminPingController 슬라이스 테스트.
 *
 * - 컨트롤러 자체 동작(200 + 응답 본문) 검증
 * - **RBAC 매트릭스(401/403)는 e2e/통합 테스트에서 검증** (D10):
 *     @WebMvcTest 슬라이스에서 @EnableMethodSecurity의 AOP 인터셉터가
 *     안정적으로 활성화되지 않아 슬라이스 단위로는 권한 거부를 시뮬레이션하기 어렵다.
 *     운영 환경에서는 SecurityConfig의 URL 기반(`/admin/** hasRole(ADMIN)`)이 1차 방어이며,
 *     실제 e2e 호출로 검증한다.
 */
@WebMvcTest(controllers = AdminPingController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminPingControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Test
    @WithMockUser(username = "admin-1", roles = "ADMIN")
    @DisplayName("ADMIN 호출 시 응답에 adminUserId 포함")
    void admin_returns_userId() throws Exception {
        mockMvc.perform(get("/admin/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminUserId").value("admin-1"))
                .andExpect(jsonPath("$.message").value("pong"))
                .andExpect(jsonPath("$.serverTime").exists());
    }
}
