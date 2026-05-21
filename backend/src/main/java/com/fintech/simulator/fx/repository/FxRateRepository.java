package com.fintech.simulator.fx.repository;

import com.fintech.simulator.fx.domain.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {

    /** (base, quote, fetched_at DESC) 인덱스로 최신 1건 조회 */
    Optional<FxRate> findFirstByBaseCurrencyAndQuoteCurrencyOrderByFetchedAtDesc(
            String baseCurrency, String quoteCurrency);
}
