package com.fintech.simulator.backtest.repository;

import com.fintech.simulator.backtest.domain.BacktestRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BacktestRunRepository extends JpaRepository<BacktestRun, Long> {
    Page<BacktestRun> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
