package com.fintech.simulator.trading.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BuyMarketRequest(
        @NotBlank String ticker,
        @NotNull @DecimalMin(value = "0.0001", message = "수량은 0보다 커야 합니다.") BigDecimal quantity
) {}
