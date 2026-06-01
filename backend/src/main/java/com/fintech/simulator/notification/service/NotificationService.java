package com.fintech.simulator.notification.service;

import com.fintech.simulator.notification.controller.NotificationResponse;
import com.fintech.simulator.notification.domain.Notification;
import com.fintech.simulator.notification.domain.NotificationType;
import com.fintech.simulator.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 알림 — 적재(영속) + STOMP 사용자별 실시간 푸시.
 *
 * <p>이벤트 소스(가격알림 트리거 / 지정가 체결 / AI 코멘트)가 {@link #notify}를 호출하면
 * DB에 저장하고, 접속 중인 사용자에게 {@code /user/queue/notifications}로 즉시 푸시한다.
 * 미접속 사용자는 푸시가 드롭되어도 DB에 남아 다음 접속/폴링 시 표시된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String USER_QUEUE = "/queue/notifications";
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    /** 알림 적재 + 실시간 푸시. 별도 트랜잭션(이벤트 핸들러에서 호출). */
    @Transactional
    public void notify(String userId, NotificationType type, String title, String body, String link) {
        Notification saved = repository.save(Notification.of(userId, type, title, body, link));
        try {
            messagingTemplate.convertAndSendToUser(userId, USER_QUEUE, NotificationResponse.from(saved));
        } catch (Exception e) {
            // 푸시 실패해도 DB엔 남음 — 폴링/재접속 시 노출
            log.debug("Notification push skipped user={}: {}", userId, e.toString());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(String userId, int page, int size) {
        int safe = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), safe))
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return repository.countByUserIdAndIsRead(userId, 0);
    }

    @Transactional
    public void markRead(String userId, Long id) {
        repository.markRead(id, userId);
    }

    @Transactional
    public void markAllRead(String userId) {
        repository.markAllRead(userId);
    }
}
