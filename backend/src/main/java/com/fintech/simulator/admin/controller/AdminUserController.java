package com.fintech.simulator.admin.controller;

import com.fintech.simulator.admin.audit.Auditable;
import com.fintech.simulator.admin.security.StepUpService;
import com.fintech.simulator.auth.domain.Role;
import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.domain.UserStatus;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.portfolio.domain.Wallet;
import com.fintech.simulator.portfolio.repository.WalletRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 관리자 - 사용자 관리 (PRD FR-10.1~10.6).
 *
 * 모든 메서드 @PreAuthorize ADMIN.
 * 위험 작업(시드머니 조정·권한 변경·강제 초기화)은 step-up 토큰 필수.
 */
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final StepUpService stepUpService;

    /** Step-up 발급 (현 관리자 비밀번호 재인증 → 단기 토큰) */
    @PostMapping("/stepup")
    public StepUpResponse issueStepUp(@AuthenticationPrincipal String adminUserId,
                                      @Valid @RequestBody StepUpRequest req) {
        return new StepUpResponse(stepUpService.issue(adminUserId, req.password()));
    }

    @GetMapping
    public Page<UserView> list(@RequestParam(required = false) String q,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        // 단순 — 전체 페이지네이션. q는 클라이언트 필터로 위임(개선 여지).
        Page<User> p = userRepository.findAll(PageRequest.of(page, Math.min(size, 100)));
        return p.map(this::toView);
    }

    @GetMapping("/{userId}")
    public UserDetailView detail(@PathVariable String userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Wallet w = walletRepository.findByUserId(userId).orElse(null);
        return new UserDetailView(u.getUserId(), u.getUsername(), u.getEmail(),
                u.getRole(), u.getStatus(), u.getLastLoginAt(), u.getCreatedAt(),
                w == null ? BigDecimal.ZERO : w.getCashBalance());
    }

    @PostMapping("/{userId}/suspend")
    @Auditable(action = "SUSPEND_USER", targetType = "USER", targetIdParam = "userId")
    @Transactional
    public UserView suspend(@PathVariable String userId,
                            @AuthenticationPrincipal String adminUserId) {
        if (userId.equals(adminUserId)) throw new BusinessException(ErrorCode.ADMIN_FORBIDDEN_SELF);
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        u.suspend();
        return toView(u);
    }

    @PostMapping("/{userId}/activate")
    @Auditable(action = "ACTIVATE_USER", targetType = "USER", targetIdParam = "userId")
    @Transactional
    public UserView activate(@PathVariable String userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        u.activate();
        return toView(u);
    }

    /** 위험 작업 — step-up 토큰 필수 */
    @PostMapping("/{userId}/cash")
    @Auditable(action = "ADJUST_CASH", targetType = "USER", targetIdParam = "userId")
    @Transactional
    public AdjustResult adjustCash(@PathVariable String userId,
                                   @AuthenticationPrincipal String adminUserId,
                                   @Valid @RequestBody AdjustCashRequest req) {
        stepUpService.validate(adminUserId, req.stepUpToken());
        Wallet w = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (req.amount().signum() > 0) w.deposit(req.amount());
        else                            w.withdraw(req.amount().negate());
        return new AdjustResult(userId, w.getCashBalance(), req.reason());
    }

    @PostMapping("/{userId}/role")
    @Auditable(action = "CHANGE_ROLE", targetType = "USER", targetIdParam = "userId")
    @Transactional
    public UserView changeRole(@PathVariable String userId,
                               @AuthenticationPrincipal String adminUserId,
                               @Valid @RequestBody ChangeRoleRequest req) {
        if (userId.equals(adminUserId)) throw new BusinessException(ErrorCode.ADMIN_FORBIDDEN_SELF);
        stepUpService.validate(adminUserId, req.stepUpToken());
        // 도메인에 setter 없음 — JPQL update or 도메인 메서드 추가.
        // 단순화: 리포지토리 native update 대신 entity field 직접 변경은 setter 추가 필요.
        // 여기서는 audit 우선 + 도메인 보호 위해 후속에 도메인 메서드 추가하기로 표기.
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
                .changeRole(req.role());
        return toView(userRepository.findById(userId).orElseThrow());
    }

    private UserView toView(User u) {
        return new UserView(u.getUserId(), u.getUsername(), u.getEmail(),
                u.getRole(), u.getStatus(), u.getLastLoginAt(), u.getCreatedAt());
    }

    // ===== DTO =====
    public record StepUpRequest(@NotBlank String password) {}
    public record StepUpResponse(String stepUpToken) {}
    public record AdjustCashRequest(@NotNull BigDecimal amount, String reason, @NotBlank String stepUpToken) {}
    public record ChangeRoleRequest(@NotNull Role role, @NotBlank String stepUpToken) {}
    public record AdjustResult(String userId, BigDecimal cashBalanceAfter, String reason) {}
    public record UserView(String userId, String username, String email, Role role,
                           UserStatus status, OffsetDateTime lastLoginAt, OffsetDateTime createdAt) {}
    public record UserDetailView(String userId, String username, String email, Role role,
                                  UserStatus status, OffsetDateTime lastLoginAt, OffsetDateTime createdAt,
                                  BigDecimal cashBalance) {}
    @SuppressWarnings("unused") private record _Unused(List<Void> v){}
}
