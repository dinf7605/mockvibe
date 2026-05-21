package com.fintech.simulator.admin.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 관리자 액션을 ADMIN_AUDIT_LOGS에 자동 기록.
 *
 * - 메서드 실행 성공 후만 기록 (실패 시 ROLLBACK)
 * - targetType: 대상 도메인 (USER / STOCK / SYSTEM / ANNOUNCEMENT 등)
 * - targetIdParam: 메서드 파라미터 중 어느 것을 target_id로 쓸지 (이름)
 *   비워두면 첫 번째 파라미터 toString() 사용
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {
    String action();
    String targetType();
    String targetIdParam() default "";
}
