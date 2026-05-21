package com.fintech.simulator.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.simulator.common.dto.ErrorResponse;
import com.fintech.simulator.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 요청에 대한 401 JSON 응답.
 * JwtAuthenticationFilter가 request.setAttribute("jwtError", ...)로 남긴 상세 코드 사용.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException {
        ErrorCode ec = (ErrorCode) req.getAttribute("jwtError");
        if (ec == null) {
            ec = ErrorCode.INVALID_TOKEN;
        }
        res.setStatus(ec.getStatus().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), ErrorResponse.of(ec.getCode(), ec.getMessage()));
    }
}
