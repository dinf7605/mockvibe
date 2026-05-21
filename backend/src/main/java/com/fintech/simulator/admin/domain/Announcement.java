package com.fintech.simulator.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ANNOUNCEMENTS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "announcement_id")
    private Long announcementId;

    @Column(name = "admin_user_id", length = 50, nullable = false) private String adminUserId;
    @Column(name = "title", length = 200, nullable = false) private String title;
    @Lob @Column(name = "content", nullable = false) private String content;
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20, nullable = false) private AnnouncementLevel level;
    @Column(name = "is_active", nullable = false) private Integer isActive;
    @Column(name = "starts_at") private OffsetDateTime startsAt;
    @Column(name = "ends_at")   private OffsetDateTime endsAt;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;

    public static Announcement create(String adminUserId, String title, String content,
                                       AnnouncementLevel level, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        Announcement a = new Announcement();
        a.adminUserId = adminUserId; a.title = title; a.content = content;
        a.level = level == null ? AnnouncementLevel.INFO : level;
        a.startsAt = startsAt; a.endsAt = endsAt; a.isActive = 1;
        a.createdAt = OffsetDateTime.now();
        return a;
    }

    public void update(String title, String content, AnnouncementLevel level,
                       OffsetDateTime startsAt, OffsetDateTime endsAt) {
        this.title = title; this.content = content;
        if (level != null) this.level = level;
        this.startsAt = startsAt; this.endsAt = endsAt;
    }

    public void toggleActive(boolean active) { this.isActive = active ? 1 : 0; }
    public boolean isActive() { return isActive != null && isActive == 1; }
}
