package com.fintech.simulator.ranking.repository;

import com.fintech.simulator.ranking.domain.PortfolioSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    /** 내 자산 추이 — 오래된 → 최신 순. */
    List<PortfolioSnapshot> findByUserIdOrderBySnapshotDateAsc(String userId);

    /** UPSERT 용 — 오늘 행 존재 여부. */
    Optional<PortfolioSnapshot> findByUserIdAndSnapshotDate(String userId, LocalDate snapshotDate);

    /** 랭킹 기준일 (가장 최근 스냅샷 날짜). */
    @Query("select max(s.snapshotDate) from PortfolioSnapshot s")
    Optional<LocalDate> findLatestSnapshotDate();

    /** 특정 날짜의 수익률 내림차순 — 리더보드. */
    List<PortfolioSnapshot> findBySnapshotDateOrderByReturnPctDesc(LocalDate snapshotDate, Pageable pageable);

    /** 내 등수 계산용 — 같은 날짜에서 내 수익률보다 높은 사람 수. */
    long countBySnapshotDateAndReturnPctGreaterThan(LocalDate snapshotDate, java.math.BigDecimal returnPct);

    long countBySnapshotDate(LocalDate snapshotDate);
}
