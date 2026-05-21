package com.fintech.simulator.portfolio.repository;

import com.fintech.simulator.portfolio.domain.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(String userId);

    /**
     * 매매 트랜잭션에서 사용. SELECT ... FOR UPDATE.
     * Wallet은 충돌 빈도가 높아 비관적 락이 유리 (PRD §7.2 ADR-2).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") String userId);
}
