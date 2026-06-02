package com.fintech.simulator.market.repository;

import com.fintech.simulator.market.domain.IntradayCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface IntradayCandleRepository extends JpaRepository<IntradayCandle, Long> {

    /** UPSERT 용 — 해당 분 버킷 존재 여부. */
    Optional<IntradayCandle> findByTickerAndBucketTs(String ticker, OffsetDateTime bucketTs);

    /** 차트 조회 — since 이후 분봉, 오래된→최신. */
    List<IntradayCandle> findByTickerAndBucketTsGreaterThanEqualOrderByBucketTsAsc(
            String ticker, OffsetDateTime since);

    /** 보관 정책 — cutoff 이전 분봉 삭제 (일일 purge). */
    @Modifying
    @Query("delete from IntradayCandle c where c.bucketTs < :cutoff")
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
