package com.fintech.simulator.trading.event;

import com.fintech.simulator.trading.domain.OrderSide;

import java.math.BigDecimal;

/** TradingService가 시장가 체결 직후 발행 → AiCommentListener가 비동기로 한 줄 코멘트 생성 */
public record OrderExecutedEvent(
        String userId,
        String ticker,
        OrderSide side,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal totalAmountKrw
) {}
