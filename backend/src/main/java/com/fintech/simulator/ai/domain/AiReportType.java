package com.fintech.simulator.ai.domain;

public enum AiReportType {
    TRADE_COMMENT,   // 매매 직후 한 줄 코멘트
    WEEKLY,          // 주간 회고 (일요일 스케줄러)
    INSTANT          // 사용자가 명시적으로 요청
}
