package com.fintech.simulator.alert.repository;

import com.fintech.simulator.alert.domain.AlertStatus;
import com.fintech.simulator.alert.domain.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    /** 내 알림 목록 — 최근 생성 순. */
    List<PriceAlert> findByUserIdOrderByCreatedAtDesc(String userId);

    /** 시세 평가용 — 특정 종목의 감시중 알림. */
    List<PriceAlert> findByTickerAndStatus(String ticker, AlertStatus status);

    /** 취소 시 소유권 확인 겸 조회. */
    Optional<PriceAlert> findByAlertIdAndUserId(Long alertId, String userId);

    /** 알림 벨 배지용 — 미확인(TRIGGERED) 개수. */
    long countByUserIdAndStatus(String userId, AlertStatus status);
}
