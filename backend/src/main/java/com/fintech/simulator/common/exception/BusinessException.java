package com.fintech.simulator.common.exception;

import lombok.Getter;

/**
 * 비즈니스 도메인 예외. 모든 도메인 서비스에서 던지는 예외는 이 타입으로 통일.
 * GlobalExceptionHandler가 ErrorCode에 매핑된 HTTP 응답으로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
