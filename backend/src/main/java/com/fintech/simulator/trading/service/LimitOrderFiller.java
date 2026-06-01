package com.fintech.simulator.trading.service;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.notification.domain.NotificationType;
import com.fintech.simulator.notification.service.NotificationService;
import com.fintech.simulator.trading.domain.LimitOrder;
import com.fintech.simulator.trading.domain.OrderSide;
import com.fintech.simulator.trading.repository.LimitOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 지정가 체결 트랜잭션 (D23).
 *
 * 흐름:
 *   1) LimitOrder 조회 + matches() double-check
 *   2) TradingService.fillLimit → Wallet→Holdings→Orders 락 순서로 단일 트랜잭션 체결
 *   3) LimitOrder.markFilled(orderId) → PENDING→FILLED 전이 + filled_order_id 기록
 *
 * 전체가 단일 @Transactional. 중간 실패 시 ORDERS/Wallet/Holdings 변경과 LIMIT_ORDERS 상태가
 * 함께 롤백되어 정합성 보장.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LimitOrderFiller {

    private final LimitOrderRepository limitOrderRepository;
    private final TradingService tradingService;
    private final NotificationService notificationService;

    @Transactional
    public void fill(Long limitOrderId, BigDecimal currentPrice) {
        LimitOrder lo = limitOrderRepository.findById(limitOrderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIMIT_ORDER_NOT_FOUND));

        if (!lo.matches(currentPrice)) return;

        // 사용자 의도 보존: 실제 체결가는 target_price (현재가가 더 유리해도 target에 체결)
        Long orderId = tradingService.fillLimit(
                lo.getUserId(), lo.getTicker(), lo.getOrderType(),
                lo.getTargetPrice(), lo.getQuantity());

        lo.markFilled(orderId);

        String sideLabel = lo.getOrderType() == OrderSide.BUY ? "매수" : "매도";
        notificationService.notify(lo.getUserId(), NotificationType.LIMIT_FILL,
                String.format("지정가 %s 체결 · %s", sideLabel, lo.getTicker()),
                String.format("%s주 @ %s 체결되었습니다.", lo.getQuantity(), lo.getTargetPrice()),
                "/stocks/" + lo.getTicker());

        log.info("Limit FILLED: id={} ticker={} {} qty={}@{} orderId={}",
                lo.getLimitOrderId(), lo.getTicker(), lo.getOrderType(),
                lo.getQuantity(), lo.getTargetPrice(), orderId);
    }
}
