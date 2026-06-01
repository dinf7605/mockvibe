package com.fintech.simulator.notification.controller;

import com.fintech.simulator.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통합 알림 센터 API (로그인 필요).
 *
 * <ul>
 *   <li>GET  /notifications?page&size  — 내 알림 목록</li>
 *   <li>GET  /notifications/unread-count — 미확인 개수 (벨 배지)</li>
 *   <li>POST /notifications/{id}/read   — 단건 확인</li>
 *   <li>POST /notifications/read-all     — 전체 확인</li>
 * </ul>
 * 실시간 수신은 STOMP {@code /user/queue/notifications} 구독.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Page<NotificationResponse> list(@AuthenticationPrincipal String userId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return notificationService.list(userId, page, size);
    }

    @GetMapping("/unread-count")
    public UnreadResponse unreadCount(@AuthenticationPrincipal String userId) {
        return new UnreadResponse(notificationService.unreadCount(userId));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal String userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal String userId) {
        notificationService.markAllRead(userId);
    }

    public record UnreadResponse(long count) {}
}
