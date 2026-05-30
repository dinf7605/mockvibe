package com.fintech.simulator.alert.controller;

import com.fintech.simulator.alert.domain.AlertDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateAlertRequest(
        @NotBlank String ticker,
        @NotNull AlertDirection direction,
        @NotNull @Positive BigDecimal targetPrice
) {}
