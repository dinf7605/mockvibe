package com.fintech.simulator.trading.scheduler;

import com.fintech.simulator.trading.domain.LimitOrder;
import com.fintech.simulator.trading.domain.LimitOrderStatus;
import com.fintech.simulator.trading.repository.LimitOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 지정가 주문 만료 배치 (D24).
 *
 * - 매일 자정(서버 시간) PENDING 중 expires_at 지난 건 EXPIRED 전이
 * - 인덱스 idx_lo_status_expires 활용 (D21 V3)
 * - 부팅 직후에도 1회 실행하여 다운타임 중 만료 누락 보정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LimitOrderExpiryScheduler {

    private final LimitOrderRepository limitOrderRepository;

    /** 매일 자정 0:00:00 (서버 KST) */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runDaily() { runOnce(); }

    /** 부팅 1분 후 (다운타임 보정) — 운영에서 한 번만 */
    @Scheduled(initialDelay = 60_000L, fixedDelay = Long.MAX_VALUE)
    public void runOnBoot() { runOnce(); }

    @Transactional
    public int runOnce() {
        List<LimitOrder> targets = limitOrderRepository
                .findByStatusAndExpiresAtLessThan(LimitOrderStatus.PENDING, OffsetDateTime.now());
        int expired = 0;
        for (LimitOrder lo : targets) {
            try {
                lo.expire();
                expired++;
            } catch (Exception ignored) {
                // 다른 스레드가 먼저 처리한 경우 (LIMIT_ORDER_NOT_PENDING) — skip
            }
        }
        if (expired > 0) log.info("Limit orders expired: {} (scanned={})", expired, targets.size());
        return expired;
    }
}
