package com.fintech.simulator.trading.dto;

import com.fintech.simulator.market.service.PriceLookupService.PriceSource;
import com.fintech.simulator.trading.domain.Order;
import com.fintech.simulator.trading.domain.OrderMethod;
import com.fintech.simulator.trading.domain.OrderSide;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        OffsetDateTime createdAt,
        /** 체결가의 출처. LIVE 면 실시간 시세, CLOSE 면 장 외 시간의 최근 종가 폴백. */
        PriceSource priceSource,
        /** priceSource=CLOSE 일 때 그 종가의 거래일. LIVE 면 null. */
        LocalDate priceAsOfDate
) {
    public static OrderResponse from(Order o, BigDecimal walletBalanceAfter,
                                     PriceSource source, LocalDate asOfDate) {
        return new OrderResponse(
                o.getOrderId(), o.getTicker(), o.getOrderType(), o.getOrderMethod(),
                o.getPrice(), o.getQuantity(), o.getFxRate(), o.getFee(),
                o.getTotalAmountKrw(), walletBalanceAfter, o.getCreatedAt(),
                source, asOfDate
        );
    }
}
