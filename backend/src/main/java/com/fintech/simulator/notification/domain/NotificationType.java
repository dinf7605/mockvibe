package com.fintech.simulator.notification.domain;

/** 알림 종류. */
public enum NotificationType {
    /** 가격 알림 목표가 도달 */
    PRICE_ALERT,
    /** 지정가 주문 체결 */
    LIMIT_FILL,
    /** AI 매매 코멘트 */
    AI_COMMENT
}
