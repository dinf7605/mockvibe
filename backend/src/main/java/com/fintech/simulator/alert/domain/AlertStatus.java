package com.fintech.simulator.alert.domain;

/** 가격 알림 상태. */
public enum AlertStatus {
    /** 감시 중. */
    ACTIVE,
    /** 목표가 도달 — 트리거됨. */
    TRIGGERED,
    /** 사용자가 취소. */
    CANCELLED
}
