package com.fintech.simulator.notification.repository;

import com.fintech.simulator.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /** 미확인 개수 (벨 배지). */
    long countByUserIdAndIsRead(String userId, int isRead);

    /** 단건 확인 처리 — 본인 것만. @return 영향 행 수(0이면 없거나 타인 것). */
    @Modifying
    @Query("update Notification n set n.isRead = 1 where n.notificationId = :id and n.userId = :userId")
    int markRead(@Param("id") Long id, @Param("userId") String userId);

    /** 전체 확인 처리. */
    @Modifying
    @Query("update Notification n set n.isRead = 1 where n.userId = :userId and n.isRead = 0")
    int markAllRead(@Param("userId") String userId);
}
