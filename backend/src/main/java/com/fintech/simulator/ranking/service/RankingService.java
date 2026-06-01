package com.fintech.simulator.ranking.service;

import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.ranking.controller.RankingResponse;
import com.fintech.simulator.ranking.controller.RankingResponse.RankingEntry;
import com.fintech.simulator.ranking.controller.TrendPoint;
import com.fintech.simulator.ranking.domain.PortfolioSnapshot;
import com.fintech.simulator.ranking.repository.PortfolioSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 수익률 랭킹 + 내 자산 추이 조회. 적재는 {@link PortfolioSnapshotService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private static final int MAX_LIMIT = 100;

    private final PortfolioSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;

    public RankingResponse ranking(String userId, int limit) {
        int n = Math.max(1, Math.min(limit, MAX_LIMIT));
        Optional<LocalDate> latest = snapshotRepository.findLatestSnapshotDate();
        if (latest.isEmpty()) {
            return new RankingResponse(List.of(), null, null, null, 0);
        }
        LocalDate date = latest.get();

        List<PortfolioSnapshot> top = snapshotRepository
                .findBySnapshotDateOrderByReturnPctDesc(date, PageRequest.of(0, n));

        // userId → username 한 번에 조회
        Map<String, String> names = userRepository.findAllById(
                top.stream().map(PortfolioSnapshot::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getUserId, User::getUsername, (a, b) -> a));

        List<RankingEntry> entries = new java.util.ArrayList<>();
        int rank = 1;
        for (PortfolioSnapshot s : top) {
            entries.add(new RankingEntry(rank++, names.getOrDefault(s.getUserId(), "사용자"),
                    s.getReturnPct(), s.getTotalAssetKrw()));
        }

        // 내 등수
        Integer myRank = null;
        BigDecimal myReturn = null;
        Optional<PortfolioSnapshot> mine = snapshotRepository.findByUserIdAndSnapshotDate(userId, date);
        if (mine.isPresent()) {
            myReturn = mine.get().getReturnPct();
            myRank = (int) snapshotRepository.countBySnapshotDateAndReturnPctGreaterThan(date, myReturn) + 1;
        }
        long total = snapshotRepository.countBySnapshotDate(date);

        return new RankingResponse(entries, date, myRank, myReturn, total);
    }

    public List<TrendPoint> myTrend(String userId) {
        return snapshotRepository.findByUserIdOrderBySnapshotDateAsc(userId).stream()
                .map((Function<PortfolioSnapshot, TrendPoint>) s ->
                        new TrendPoint(s.getSnapshotDate(), s.getTotalAssetKrw(), s.getReturnPct()))
                .toList();
    }
}
