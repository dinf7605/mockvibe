package com.fintech.simulator.trading.dto;

import com.fintech.simulator.trading.domain.Order;
import com.fintech.simulator.trading.domain.OrderMethod;
import com.fintech.simulator.trading.domain.OrderSide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderResponse(
        Long orderId,
        String ticker,
        OrderSide orderType,
        OrderMethod orderMethod,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal fxRate,
        BigDecimal fee,
        BigDecimal totalAmountKrw,
        BigDecimal walletBalanceAfterKrw,
        OffsetDateTime createdAt
) {
    public static OrderResponse from(Order o, BigDecimal walletBalanceAfter) {
        return new OrderResponse(
                o.getOrderId(), o.getTicker(), o.getOrderType(), o.getOrderMethod(),
                o.getPrice(), o.getQuantity(), o.getFxRate(), o.getFee(),
                o.getTotalAmountKrw(), walletBalanceAfter, o.getCreatedAt()
        );
    }
}
