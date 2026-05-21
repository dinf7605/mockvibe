package com.fintech.simulator.common.exception;

import com.fintech.simulator.common.dto.ErrorResponse;
import com.fintech.simulator.common.dto.ErrorResponse.FieldError;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorCode ec = ex.getErrorCode();
        log.warn("Business: [{}] {}", ec.getCode(), ex.getMessage());
        return ResponseEntity.status(ec.getStatus())
                .body(ErrorResponse.of(ec.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                .toList();
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(ec.getStatus())
                .body(ErrorResponse.of(ec.getCode(), ec.getMessage(), fields));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(ec.getStatus())
                .body(ErrorResponse.of(ec.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("DataIntegrity: {}", ex.getMostSpecificCause().getMessage());
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(ec.getStatus())
                .body(ErrorResponse.of(ec.getCode(), "데이터 무결성 제약을 위반했습니다."));
    }

    /** 본문 JSON 파싱 실패 (잘못된 형식·인코딩) → 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Body parse failed: {}", ex.getMessage());
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(ec.getStatus())
                .body(ErrorResponse.of(ec.getCode(), "요청 본문을 해석할 수 없습니다. JSON 형식·UTF-8 인코딩을 확인하세요."));
    }

    /** 허용되지 않은 HTTP 메서드 → 405 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ErrorResponse.of("COMMON_405", "허용되지 않은 HTTP 메서드입니다: " + ex.getMethod()));
    }

    /** 매핑되지 않은 경로 → 404 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ErrorResponse.of("COMMON_404", "요청한 경로를 찾을 수 없습니다."));
    }

    /**
     * @PreAuthorize 등에서 권한 부족 → 403.
     * 운영에서는 보통 JwtAccessDeniedHandler가 먼저 잡지만,
     * 컨트롤러 메서드 내부에서 던져진 경우의 fallback.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("AUTH_007", "접근 권한이 없습니다."));
    }

    /**
     * 익명 사용자가 보호된 자원에 접근 시 @PreAuthorize가 던지는 예외 → 401.
     * (운영에서는 JwtAuthenticationEntryPoint가 먼저 잡음)
     */
    @ExceptionHandler({AuthenticationException.class, AuthenticationCredentialsNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        ErrorCode ec = ErrorCode.INVALID_TOKEN;
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ec.getCode(), "인증이 필요합니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(ec.getStatus())
                .body(ErrorResponse.of(ec.getCode(), ec.getMessage()));
    }
}
