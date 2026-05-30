package com.fintech.simulator.alert.service;

import com.fintech.simulator.alert.domain.AlertDirection;
import com.fintech.simulator.alert.domain.AlertStatus;
import com.fintech.simulator.alert.domain.PriceAlert;
import com.fintech.simulator.alert.repository.PriceAlertRepository;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 가격 알림 관리 (생성/조회/취소). 트리거 평가는 {@link PriceAlertProcessor} 가 담당.
 */
@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private final PriceAlertRepository alertRepository;
    private final StockRepository stockRepository;

    @Transactional
    public PriceAlert create(String userId, String ticker, AlertDirection direction, BigDecimal targetPrice) {
        if (direction == null || targetPrice == null || targetPrice.signum() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "방향/목표가가 올바르지 않습니다.");
        }
        if (stockRepository.findById(ticker).isEmpty()) {
            throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
        }
        return alertRepository.save(PriceAlert.create(userId, ticker, direction, targetPrice));
    }

    @Transactional(readOnly = true)
    public List<PriceAlert> list(String userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** 알림 벨 배지용 — 도달(TRIGGERED) 개수. */
    @Transactional(readOnly = true)
    public long triggeredCount(String userId) {
        return alertRepository.countByUserIdAndStatus(userId, AlertStatus.TRIGGERED);
    }

    @Transactional
    public void cancel(String userId, Long alertId) {
        PriceAlert alert = alertRepository.findByAlertIdAndUserId(alertId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALERT_NOT_FOUND));
        alert.cancel();
        alertRepository.save(alert);
    }
}
