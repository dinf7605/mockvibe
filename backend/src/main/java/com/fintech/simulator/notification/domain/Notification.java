package com.fintech.simulator.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 사용자 알림 한 건. 가격알림 도달 / 지정가 체결 / AI 코멘트 등 이벤트가 적재된다.
 */
@Entity
@Table(name = "NOTIFICATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private NotificationType type;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "body", length = 1000)
    private String body;

    @Column(name = "link", length = 200)
    private String link;

    /** 0=미확인, 1=확인 (Oracle NUMBER(1)) */
    @Column(name = "is_read", nullable = false)
    private Integer isRead;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    private Notification(String userId, NotificationType type, String title, String body, String link) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
        this.isRead = 0;
        this.createdAt = OffsetDateTime.now();
    }

    public static Notification of(String userId, NotificationType type, String title, String body, String link) {
        return new Notification(userId, type, title, body, link);
    }

    public boolean isRead() {
        return isRead != null && isRead == 1;
    }
}
