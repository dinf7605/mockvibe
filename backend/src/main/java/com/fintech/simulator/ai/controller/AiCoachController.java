package com.fintech.simulator.ai.controller;

import com.fintech.simulator.ai.domain.AiReport;
import com.fintech.simulator.ai.domain.AiReportType;
import com.fintech.simulator.ai.repository.AiReportRepository;
import com.fintech.simulator.ai.service.AiCoachService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiCoachController {

    private final AiCoachService aiCoachService;
    private final AiReportRepository aiReportRepository;

    @PostMapping("/analyze")
    public AiReportView analyze(@AuthenticationPrincipal String userId) {
        return AiReportView.from(aiCoachService.analyzeInstant(userId));
    }

    @GetMapping("/reports")
    public ReportList reports(@AuthenticationPrincipal String userId,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) AiReportType type) {
        Page<AiReport> p = (type == null)
                ? aiReportRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                : aiReportRepository.findByUserIdAndReportTypeOrderByCreatedAtDesc(userId, type, PageRequest.of(page, size));
        return new ReportList(p.getContent().stream().map(AiReportView::from).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    public record AiReportView(Long reportId, AiReportType reportType, String content,
                               Integer tokenUsed, OffsetDateTime createdAt) {
        public static AiReportView from(AiReport r) {
            return new AiReportView(r.getReportId(), r.getReportType(), r.getContent(),
                    r.getTokenUsed(), r.getCreatedAt());
        }
    }
    public record ReportList(List<AiReportView> items, int page, int size,
                             long totalElements, int totalPages) {}
}
