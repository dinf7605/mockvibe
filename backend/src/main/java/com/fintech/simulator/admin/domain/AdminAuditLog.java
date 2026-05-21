package com.fintech.simulator.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ADMIN_AUDIT_LOGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "admin_user_id", length = 50, nullable = false) private String adminUserId;
    @Column(name = "action", length = 50, nullable = false) private String action;
    @Column(name = "target_type", length = 50, nullable = false) private String targetType;
    @Column(name = "target_id", length = 100) private String targetId;
    @Lob @Column(name = "before_value") private String beforeValue;
    @Lob @Column(name = "after_value") private String afterValue;
    @Column(name = "reason", length = 500) private String reason;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @Column(name = "user_agent", length = 500) private String userAgent;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;

    public static AdminAuditLog of(String admin, String action, String targetType, String targetId,
                                   String before, String after, String reason,
                                   String ip, String ua) {
        AdminAuditLog a = new AdminAuditLog();
        a.adminUserId = admin; a.action = action; a.targetType = targetType; a.targetId = targetId;
        a.beforeValue = before; a.afterValue = after; a.reason = reason;
        a.ipAddress = ip; a.userAgent = ua;
        a.createdAt = OffsetDateTime.now();
        return a;
    }
}
