package com.fintech.simulator.trading.service;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.repository.StockRepository;
import com.fintech.simulator.trading.domain.LimitOrder;
import com.fintech.simulator.trading.dto.LimitOrderRequest;
import com.fintech.simulator.trading.dto.LimitOrderResponse;
import com.fintech.simulator.trading.repository.LimitOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 지정가 주문 등록·조회·취소.
 *
 * 정책: 등록 시 잔고/보유를 차감하지 않는다 (Hold 방식 X).
 *   D22 체결 시점에 Wallet/Holdings 검증을 거치며, 부족하면 그 지정가만 실패 처리.
 *   사용자가 무한 등록하지 못하도록 사용자당 최대 PENDING N개 한도(D24)는 후속.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LimitOrderService {

    private static final int DEFAULT_VALIDITY_DAYS = 30;

    private final StockRepository stockRepository;
    private final LimitOrderRepository limitOrderRepository;

    @Transactional
    public LimitOrderResponse register(String userId, LimitOrderRequest req) {
        Stock stock = stockRepository.findById(req.ticker())
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
        if (!stock.isActive()) throw new BusinessException(ErrorCode.STOCK_INACTIVE);

        int days = req.validityDays() != null && req.validityDays() > 0
                ? req.validityDays() : DEFAULT_VALIDITY_DAYS;
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(days);

        LimitOrder order = LimitOrder.register(
                userId, req.ticker(), req.orderType(), req.targetPrice(), req.quantity(), expiresAt);
        limitOrderRepository.save(order);

        log.info("Limit order registered: id={}, user={}, {} {} {}@{}",
                order.getLimitOrderId(), userId, req.orderType(), req.ticker(), req.quantity(), req.targetPrice());
        return LimitOrderResponse.from(order);
    }

    @Transactional
    public void cancel(String userId, Long limitOrderId) {
        LimitOrder order = limitOrderRepository.findById(limitOrderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIMIT_ORDER_NOT_FOUND));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.LIMIT_ORDER_FORBIDDEN);
        }
        order.cancel();
    }

    @Transactional(readOnly = true)
    public LimitOrderResponse.Page_ list(String userId, int page, int size) {
        int safe = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safe);
        return LimitOrderResponse.Page_.from(
                limitOrderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable));
    }
}
