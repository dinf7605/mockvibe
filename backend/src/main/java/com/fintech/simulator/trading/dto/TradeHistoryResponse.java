package com.fintech.simulator.trading.dto;

import com.fintech.simulator.trading.domain.Order;
import com.fintech.simulator.trading.domain.OrderMethod;
import com.fintech.simulator.trading.domain.OrderSide;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TradeHistoryResponse(
        List<Item> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public record Item(
            Long orderId,
            String ticker,
            OrderSide orderType,
            OrderMethod orderMethod,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal fxRate,
            BigDecimal fee,
            BigDecimal totalAmountKrw,
            OffsetDateTime createdAt
    ) {
        public static Item from(Order o) {
            return new Item(o.getOrderId(), o.getTicker(), o.getOrderType(), o.getOrderMethod(),
                    o.getPrice(), o.getQuantity(), o.getFxRate(), o.getFee(),
                    o.getTotalAmountKrw(), o.getCreatedAt());
        }
    }

    public static TradeHistoryResponse from(Page<Order> p) {
        return new TradeHistoryResponse(
                p.getContent().stream().map(Item::from).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages()
        );
    }
}
