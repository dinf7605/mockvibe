package com.fintech.simulator.notification.controller;

import com.fintech.simulator.notification.domain.Notification;
import com.fintech.simulator.notification.domain.NotificationType;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String body,
        String link,
        boolean read,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getNotificationId(), n.getType(), n.getTitle(), n.getBody(),
                n.getLink(), n.isRead(), n.getCreatedAt());
    }
}
