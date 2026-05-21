package com.fintech.simulator.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "AI_REPORTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 20, nullable = false)
    private AiReportType reportType;

    @Column(name = "context_hash", length = 64)
    private String contextHash;

    @Lob @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "token_used", nullable = false)
    private Integer tokenUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static AiReport of(String userId, AiReportType type, String contextHash, String content, int tokenUsed) {
        AiReport r = new AiReport();
        r.userId = userId; r.reportType = type; r.contextHash = contextHash;
        r.content = content; r.tokenUsed = tokenUsed;
        r.createdAt = OffsetDateTime.now();
        return r;
    }
}
