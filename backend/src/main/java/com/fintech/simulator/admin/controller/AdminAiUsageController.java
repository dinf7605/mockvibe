package com.fintech.simulator.admin.controller;

import com.fintech.simulator.ai.repository.AiReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 관리자 - AI 비용 (PRD FR-10.18).
 * 일/월 토큰 사용량 누적 집계. 단순 — 최근 1000건 스캔하여 일자별 합산.
 * 운영 규모가 커지면 별도 집계 테이블/배치로 교체.
 */
@RestController
@RequestMapping("/admin/ai-usage")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAiUsageController {

    private final AiReportRepository aiReportRepository;

    @GetMapping("/daily")
    public Map<String, Long> daily() {
        // 최근 1000건 기준 (날짜 YYYY-MM-DD → 토큰 합)
        Map<String, Long> daily = new HashMap<>();
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(30);
        aiReportRepository.findAll(PageRequest.of(0, 1000))
                .forEach(r -> {
                    if (r.getCreatedAt().isBefore(cutoff)) return;
                    String day = r.getCreatedAt().toLocalDate().toString();
                    daily.merge(day, (long) r.getTokenUsed(), Long::sum);
                });
        return daily;
    }
}
