package com.fintech.simulator.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 에러 코드.
 * - 코드 prefix로 도메인 구분 (AUTH/USER/TRADE/MARKET/ADMIN/COMMON)
 * - HTTP 상태와 매핑
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증/회원
    EMAIL_ALREADY_EXISTS  (HttpStatus.CONFLICT,         "USER_001", "이미 가입된 이메일입니다."),
    USER_NOT_FOUND        (HttpStatus.NOT_FOUND,        "USER_002", "사용자를 찾을 수 없습니다."),
    INVALID_CREDENTIALS   (HttpStatus.UNAUTHORIZED,     "AUTH_001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_SUSPENDED     (HttpStatus.FORBIDDEN,        "AUTH_002", "정지된 계정입니다."),
    INVALID_TOKEN         (HttpStatus.UNAUTHORIZED,     "AUTH_003", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED         (HttpStatus.UNAUTHORIZED,     "AUTH_004", "토큰이 만료되었습니다."),
    TOKEN_REVOKED         (HttpStatus.UNAUTHORIZED,     "AUTH_005", "더 이상 사용할 수 없는 토큰입니다."),
    REFRESH_TOKEN_MISSING (HttpStatus.UNAUTHORIZED,     "AUTH_006", "리프레시 토큰이 없습니다."),

    // 매매
    INSUFFICIENT_BALANCE  (HttpStatus.BAD_REQUEST,      "TRADE_001", "잔고가 부족합니다."),
    INSUFFICIENT_HOLDINGS (HttpStatus.BAD_REQUEST,      "TRADE_002", "보유 수량이 부족합니다."),
    STOCK_INACTIVE        (HttpStatus.BAD_REQUEST,      "TRADE_003", "거래가 중지된 종목입니다."),
    INVALID_QUANTITY      (HttpStatus.BAD_REQUEST,      "TRADE_004", "주문 수량이 올바르지 않습니다."),
    HOLDINGS_CONFLICT     (HttpStatus.CONFLICT,         "TRADE_005", "동시 주문 충돌로 거래가 실패했습니다. 다시 시도하세요."),
    LIMIT_ORDER_NOT_FOUND (HttpStatus.NOT_FOUND,        "TRADE_007", "지정가 주문을 찾을 수 없습니다."),
    LIMIT_ORDER_NOT_PENDING(HttpStatus.BAD_REQUEST,     "TRADE_008", "이미 처리된 지정가 주문입니다."),
    LIMIT_ORDER_FORBIDDEN (HttpStatus.FORBIDDEN,        "TRADE_009", "본인의 지정가 주문만 취소할 수 있습니다."),

    // 시장 데이터
    PRICE_NOT_AVAILABLE   (HttpStatus.NOT_FOUND,        "MARKET_001", "현재 시세를 조회할 수 없습니다."),
    STOCK_NOT_FOUND       (HttpStatus.NOT_FOUND,        "MARKET_002", "존재하지 않는 종목입니다."),
    EXTERNAL_RATE_LIMITED (HttpStatus.TOO_MANY_REQUESTS, "MARKET_003", "외부 API 호출 한도를 초과했습니다."),
    EXTERNAL_API_ERROR    (HttpStatus.BAD_GATEWAY,       "MARKET_004", "외부 시장 데이터 API 호출에 실패했습니다."),
    SUBSCRIPTION_LIMIT    (HttpStatus.TOO_MANY_REQUESTS, "MARKET_005", "실시간 시세 동시 구독 한도를 초과했습니다."),

    // AI 코치
    AI_DAILY_LIMIT        (HttpStatus.TOO_MANY_REQUESTS, "AI_001", "오늘의 AI 코치 호출 한도를 초과했습니다."),
    AI_API_ERROR          (HttpStatus.BAD_GATEWAY,       "AI_002", "AI 코치 API 호출에 실패했습니다."),

    // 관리자
    STEPUP_REQUIRED       (HttpStatus.UNAUTHORIZED,     "ADMIN_001", "위험 작업에는 비밀번호 재인증이 필요합니다."),
    ADMIN_FORBIDDEN_SELF  (HttpStatus.FORBIDDEN,        "ADMIN_003", "본인 계정에는 이 작업을 수행할 수 없습니다."),
    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND,        "ADMIN_004", "공지사항을 찾을 수 없습니다."),

    // 공통
    VALIDATION_FAILED     (HttpStatus.BAD_REQUEST,      "COMMON_001", "요청 값 검증에 실패했습니다."),
    INTERNAL_ERROR        (HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
