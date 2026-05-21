package com.fintech.simulator.ai.repository;

import com.fintech.simulator.ai.domain.AiReport;
import com.fintech.simulator.ai.domain.AiReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    Page<AiReport> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /** 응답 캐싱: 같은 컨텍스트 해시가 최근에 있으면 재사용 */
    Optional<AiReport> findFirstByUserIdAndContextHashAndCreatedAtAfter(
            String userId, String contextHash, OffsetDateTime after);

    Page<AiReport> findByUserIdAndReportTypeOrderByCreatedAtDesc(
            String userId, AiReportType type, Pageable pageable);
}
