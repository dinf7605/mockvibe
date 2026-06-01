package com.fintech.simulator.ai.service;

import com.fintech.simulator.notification.domain.NotificationType;
import com.fintech.simulator.notification.service.NotificationService;
import com.fintech.simulator.trading.event.OrderExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 매매 체결 → 비동기로 AI 한 줄 코멘트 생성 + 알림.
 * - 트랜잭션 커밋 후 발행이라 매매 결과에 영향 없음
 * - 일일 한도 초과/AI 비활성 시 silently skip
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCommentListener {

    private final AiCoachService aiCoachService;
    private final NotificationService notificationService;

    @Async
    @EventListener
    public void on(OrderExecutedEvent e) {
        try {
            aiCoachService.commentOnTrade(e.userId(), e.side(), e.ticker(),
                            e.price(), e.quantity(), e.totalAmountKrw())
                    .ifPresent(report -> notificationService.notify(
                            e.userId(), NotificationType.AI_COMMENT,
                            "AI 코멘트 · " + e.ticker(),
                            report.getContent(),
                            "/stocks/" + e.ticker()));
        } catch (Exception ex) {
            log.debug("AI comment skipped: {}", ex.getMessage());
        }
    }
}
