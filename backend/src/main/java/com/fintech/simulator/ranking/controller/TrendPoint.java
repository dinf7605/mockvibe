package com.fintech.simulator.ranking.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 내 자산 추이 한 점. */
public record TrendPoint(
        LocalDate date,
        BigDecimal totalAssetKrw,
        BigDecimal returnPct
) {}
