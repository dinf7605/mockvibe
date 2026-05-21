package com.fintech.simulator.trading.dto;

import com.fintech.simulator.trading.domain.OrderSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LimitOrderRequest(
        @NotBlank String ticker,
        @NotNull OrderSide orderType,
        @NotNull @DecimalMin(value = "0.0001", message = "지정가는 0보다 커야 합니다.") BigDecimal targetPrice,
        @NotNull @DecimalMin(value = "0.0001", message = "수량은 0보다 커야 합니다.") BigDecimal quantity,
        /** 미설정 시 기본 30일 (PRD FR-3.8) */
        Integer validityDays
) {}
