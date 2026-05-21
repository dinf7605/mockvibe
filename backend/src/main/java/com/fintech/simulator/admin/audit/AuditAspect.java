package com.fintech.simulator.admin.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.simulator.admin.domain.AdminAuditLog;
import com.fintech.simulator.admin.repository.AdminAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Parameter;

/**
 * @Auditable 메서드 자동 감사 로그.
 *
 *   - 성공 시에만 INSERT (실패 시 트랜잭션 롤백 → 로그도 함께 롤백)
 *   - 현재 admin = SecurityContext의 Authentication.getName()
 *   - IP/UA = 현재 HTTP 요청 헤더
 *   - before/after는 메서드가 반환하는 객체를 after에 직렬화 (간단화)
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AdminAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        Object result = pjp.proceed();
        try {
            String admin = currentAdmin();
            String targetId = resolveTargetId(pjp, auditable);
            String after = serialize(result);
            HttpServletRequest req = currentRequest();
            String ip = req != null ? req.getRemoteAddr() : null;
            String ua = req != null ? req.getHeader(HttpHeaders.USER_AGENT) : null;

            repository.save(AdminAuditLog.of(admin, auditable.action(),
                    auditable.targetType(), targetId, null, after, null, ip, ua));
        } catch (Exception logEx) {
            log.warn("Audit log write failed (action={}): {}", auditable.action(), logEx.getMessage());
            // 감사 로그 실패가 비즈니스 로직을 막지 않게 swallow
        }
        return result;
    }

    private String currentAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    private String resolveTargetId(ProceedingJoinPoint pjp, Auditable auditable) {
        Object[] args = pjp.getArgs();
        if (args.length == 0) return null;
        Parameter[] params = ((MethodSignature) pjp.getSignature()).getMethod().getParameters();

        if (!auditable.targetIdParam().isBlank()) {
            for (int i = 0; i < params.length; i++) {
                if (auditable.targetIdParam().equals(params[i].getName())) {
                    return args[i] != null ? args[i].toString() : null;
                }
            }
            return null;
        }
        // 명시 안 했으면 첫 번째 비-@AuthenticationPrincipal 파라미터를 target_id로 사용.
        // (관리자 본인 식별자는 admin_user_id 컬럼에 이미 기록되므로 중복 방지)
        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(AuthenticationPrincipal.class)) continue;
            return args[i] != null ? args[i].toString() : null;
        }
        return null;
    }

    private String serialize(Object o) {
        if (o == null) return null;
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { return o.toString(); }
    }
}
