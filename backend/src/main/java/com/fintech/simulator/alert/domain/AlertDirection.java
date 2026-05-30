package com.fintech.simulator.alert.domain;

/** 가격 알림 방향. */
public enum AlertDirection {
    /** 현재가가 목표가 이상으로 올라오면 트리거. */
    ABOVE,
    /** 현재가가 목표가 이하로 내려가면 트리거. */
    BELOW
}
