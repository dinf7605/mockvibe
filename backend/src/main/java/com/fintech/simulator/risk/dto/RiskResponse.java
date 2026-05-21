package com.fintech.simulator.risk.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 포트폴리오 리스크 지표 스냅샷.
 * - var95/var99: 1일 손실 비율 (0.025 = 2.5%)
 * - sharpe / beta: 연율
 * - mdd: 최대 낙폭 (0~1)
 * - concentration: 단일 종목 최대 비중 (0~1)
 * - regionShare/sectorShare: 비중 분포 (0~1)
 * - warnings: 임계치 초과 한국어 경고 목록
 */
public record RiskResponse(
        BigDecimal var95,
        BigDecimal var99,
        BigDecimal sharpe,
        BigDecimal beta,
        BigDecimal mdd,
        BigDecimal concentration,
        Map<String, BigDecimal> regionShare,
        Map<String, BigDecimal> sectorShare,
        List<String> warnings
) {}
