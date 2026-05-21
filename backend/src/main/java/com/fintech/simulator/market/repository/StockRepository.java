package com.fintech.simulator.market.repository;

import com.fintech.simulator.market.domain.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, String> {

    /**
     * 활성 종목 검색.
     * - 검색어가 비어있으면 전체
     * - market 필터: null이면 전체, 아니면 정확 매치
     */
    @Query("""
            SELECT s FROM Stock s
            WHERE s.isActive = 1
              AND (:q IS NULL OR :q = ''
                   OR LOWER(s.ticker)       LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.companyName)  LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:market IS NULL OR :market = '' OR s.market = :market)
            """)
    Page<Stock> search(@Param("q") String q, @Param("market") String market, Pageable pageable);
}
