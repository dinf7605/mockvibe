package com.fintech.simulator.admin.controller;

import com.fintech.simulator.admin.domain.AdminAuditLog;
import com.fintech.simulator.admin.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 - 감사 로그 조회 (PRD FR-10.21). INSERT-only 보장은 AOP가 담당. */
@RestController
@RequestMapping("/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditLogRepository repository;

    @GetMapping
    public Page<AdminAuditLog> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "50") int size,
                                    @RequestParam(required = false) String targetType,
                                    @RequestParam(required = false) String targetId) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 200));
        if (targetType != null && targetId != null) {
            return repository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId, pageable);
        }
        return repository.findByOrderByCreatedAtDesc(pageable);
    }
}
