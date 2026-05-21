package com.fintech.simulator.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * 관리자 RBAC 동작 확인용 더미 엔드포인트.
 *
 * 깊이 방어:
 *   - SecurityConfig URL 매칭으로 1차 (`/admin/**` hasRole ADMIN)
 *   - 메서드 단위 @PreAuthorize로 2차 (실수로 SecurityConfig 매칭이 깨져도 보호)
 *
 * 식별자 추출:
 *   - SecurityContextHolder를 직접 사용 (ArgumentResolver 의존성 회피).
 *   - getName()은 principal이 String이든 UserDetails든 일관된 결과 반환.
 */
@RestController
@RequestMapping("/admin")
public class AdminPingController {

    @GetMapping("/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public PingResponse ping() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (auth != null) ? auth.getName() : null;
        return new PingResponse(userId, OffsetDateTime.now(), "pong");
    }

    public record PingResponse(String adminUserId, OffsetDateTime serverTime, String message) {}
}
