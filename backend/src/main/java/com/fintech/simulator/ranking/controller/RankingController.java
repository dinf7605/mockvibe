package com.fintech.simulator.ranking.controller;

import com.fintech.simulator.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 수익률 랭킹 + 내 자산 추이 (로그인 필요).
 *
 * <ul>
 *   <li>GET /ranking?limit=20    — 리더보드 + 내 등수</li>
 *   <li>GET /ranking/me/trend    — 내 자산 추이</li>
 * </ul>
 */
@RestController
@RequestMapping("/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public RankingResponse ranking(@AuthenticationPrincipal String userId,
                                   @RequestParam(defaultValue = "20") int limit) {
        return rankingService.ranking(userId, limit);
    }

    @GetMapping("/me/trend")
    public List<TrendPoint> myTrend(@AuthenticationPrincipal String userId) {
        return rankingService.myTrend(userId);
    }
}
