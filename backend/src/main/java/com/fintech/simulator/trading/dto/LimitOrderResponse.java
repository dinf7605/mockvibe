package com.fintech.simulator.trading.dto;

import com.fintech.simulator.trading.domain.LimitOrder;
import com.fintech.simulator.trading.domain.LimitOrderStatus;
import com.fintech.simulator.trading.domain.OrderSide;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record LimitOrderResponse(
        Long limitOrderId,
        String ticker,
        OrderSide orderType,
        BigDecimal targetPrice,
        BigDecimal quantity,
        LimitOrderStatus status,
        OffsetDateTime expiresAt,
        OffsetDateTime filledAt,
        OffsetDateTime cancelledAt,
        Long filledOrderId,
        OffsetDateTime createdAt
) {
    public static LimitOrderResponse from(LimitOrder o) {
        return new LimitOrderResponse(
                o.getLimitOrderId(), o.getTicker(), o.getOrderType(), o.getTargetPrice(),
                o.getQuantity(), o.getStatus(), o.getExpiresAt(), o.getFilledAt(),
                o.getCancelledAt(), o.getFilledOrderId(), o.getCreatedAt());
    }

    public record Page_(List<LimitOrderResponse> items, int page, int size,
                        long totalElements, int totalPages) {
        public static Page_ from(Page<LimitOrder> p) {
            return new Page_(p.getContent().stream().map(LimitOrderResponse::from).toList(),
                    p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }
}
