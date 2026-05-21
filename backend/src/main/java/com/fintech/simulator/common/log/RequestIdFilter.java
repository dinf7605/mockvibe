package com.fintech.simulator.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 requestId(UUID) 부여.
 *
 * - 입력: 클라이언트가 X-Request-Id 헤더로 보냈으면 그대로 사용 (분산 추적 친화)
 * - MDC.put("requestId", ...) → 로그 패턴 `%X{requestId:-}`에 자동 출력
 * - 응답 헤더 X-Request-Id로 echo → 클라이언트가 장애 보고 시 식별자 제공
 * - finally에서 MDC.remove로 스레드 풀 누수 방지
 *
 * Security 필터보다 앞에 두어 인증 전 단계 로그도 requestId가 찍히게 한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String incoming = req.getHeader(HEADER);
        String id = (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString().substring(0, 8)
                : incoming;
        MDC.put(MDC_KEY, id);
        res.setHeader(HEADER, id);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
