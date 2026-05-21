package com.fintech.simulator.ai.service;

import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.domain.UserStatus;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.trading.domain.OrderSide;
import com.fintech.simulator.trading.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * 매주 일요일 자정(KST) — 전체 활성 사용자에게 AI 주간 회고 리포트 생성 (PRD FR-7.2).
 *
 * - 사용자별 한도(일 10회)와는 별개로 시스템이 생성하므로 limiter 우회
 * - 사용자가 매매 0건이어도 회고 생성 (단순화)
 * - 운영 안정성: 한 사용자 실패해도 다음 사용자 계속
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AiCoachService aiCoachService;

    @Scheduled(cron = "0 0 0 ? * SUN", zone = "Asia/Seoul")
    public void runWeekly() {
        OffsetDateTime weekStart = OffsetDateTime.now().minusDays(7);
        int success = 0, failure = 0;
        for (User user : userRepository.findAll()) {
            if (user.getStatus() != UserStatus.ACTIVE) continue;
            try {
                long buys  = orderRepository.findAll().stream()
                        .filter(o -> o.getUserId().equals(user.getUserId()))
                        .filter(o -> o.getOrderType() == OrderSide.BUY)
                        .filter(o -> o.getCreatedAt().isAfter(weekStart))
                        .count();
                long sells = orderRepository.findAll().stream()
                        .filter(o -> o.getUserId().equals(user.getUserId()))
                        .filter(o -> o.getOrderType() == OrderSide.SELL)
                        .filter(o -> o.getCreatedAt().isAfter(weekStart))
                        .count();
                aiCoachService.weekly(user.getUserId(), buys, sells);
                success++;
            } catch (Exception e) {
                log.warn("Weekly report failed: user={} {}", user.getUserId(), e.getMessage());
                failure++;
            }
        }
        log.info("Weekly AI reports: success={} failure={}", success, failure);
    }
}
