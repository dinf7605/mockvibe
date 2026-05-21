package com.fintech.simulator.common.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * API 에러 응답 표준 포맷.
 * fields는 입력 값 검증 실패 시 필드별 메시지 목록.
 */
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> fields,
        OffsetDateTime timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), OffsetDateTime.now());
    }

    public static ErrorResponse of(String code, String message, List<FieldError> fields) {
        return new ErrorResponse(code, message, fields, OffsetDateTime.now());
    }

    public record FieldError(String field, String message, Object rejectedValue) {}
}
