package com.fintech.simulator.ranking.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 수익률 리더보드.
 *
 * @param entries          상위 N (rank 오름차순)
 * @param asOf             기준 스냅샷 날짜 (없으면 null)
 * @param myRank           내 등수 (스냅샷 없으면 null)
 * @param myReturnPct      내 수익률 %
 * @param totalParticipants 기준일 참가자 수
 */
public record RankingResponse(
        List<RankingEntry> entries,
        LocalDate asOf,
        Integer myRank,
        BigDecimal myReturnPct,
        long totalParticipants
) {
    public record RankingEntry(
            int rank,
            String username,
            BigDecimal returnPct,
            BigDecimal totalAssetKrw
    ) {}
}
