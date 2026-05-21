package com.fintech.simulator.backtest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BacktestRequest(
        @NotBlank String strategy,    // BUY_AND_HOLD | MOVING_AVERAGE_20 | RSI_14
        @NotBlank String ticker,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin("1000.00") BigDecimal initialCapital
) {}
