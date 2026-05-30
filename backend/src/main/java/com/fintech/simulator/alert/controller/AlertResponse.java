package com.fintech.simulator.alert.controller;

import com.fintech.simulator.alert.domain.AlertDirection;
import com.fintech.simulator.alert.domain.AlertStatus;
import com.fintech.simulator.alert.domain.PriceAlert;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AlertResponse(
        Long alertId,
        String ticker,
        AlertDirection direction,
        BigDecimal targetPrice,
        AlertStatus status,
        BigDecimal triggeredPrice,
        OffsetDateTime createdAt,
        OffsetDateTime triggeredAt
) {
    public static AlertResponse from(PriceAlert a) {
        return new AlertResponse(
                a.getAlertId(), a.getTicker(), a.getDirection(), a.getTargetPrice(),
                a.getStatus(), a.getTriggeredPrice(), a.getCreatedAt(), a.getTriggeredAt()
        );
    }
}
