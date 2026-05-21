package com.fintech.simulator.portfolio.repository;

import com.fintech.simulator.portfolio.domain.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    Optional<Holding> findByUserIdAndTicker(String userId, String ticker);

    List<Holding> findAllByUserId(String userId);
}
