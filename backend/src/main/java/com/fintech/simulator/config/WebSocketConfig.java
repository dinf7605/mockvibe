package com.fintech.simulator.config;

import com.fintech.simulator.auth.jwt.JwtTokenProvider;
import com.fintech.simulator.auth.jwt.TokenClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;

/**
 * STOMP 메시지 브로커 설정.
 *
 *  - 엔드포인트: ws://host/ws (SockJS X — 순수 WebSocket)
 *  - 토픽: /topic/price/{ticker} → 종목별 시세 push (PriceBroadcaster, 공개)
 *  - 유저 큐: /user/queue/notifications → 사용자별 알림 push (NotificationService)
 *  - Heartbeat: 클라/서버 15초 (PRD FR-5.5)
 *
 * <h3>인증</h3>
 * CONNECT 프레임의 {@code Authorization: Bearer <JWT>} 를 파싱해 세션 Principal(=userId) 설정.
 * 토큰이 없거나 유효하지 않으면 <b>익명으로 통과</b> — 공개 시세 토픽은 계속 동작하고,
 * /user/* 목적지(알림)만 인증된 세션에 한정된다.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final long HEARTBEAT_MS = 15_000L;

    private final CorsProperties corsProperties;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{HEARTBEAT_MS, HEARTBEAT_MS})
                .setTaskScheduler(stompHeartbeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = corsProperties.allowedOrigins();
        String[] originArray = origins.isEmpty() ? new String[]{"*"} : origins.toArray(String[]::new);
        registry.addEndpoint("/ws").setAllowedOriginPatterns(originArray);
    }

    /**
     * CONNECT 시 JWT → Principal(userId) 설정. 사용자별 알림 push(/user/queue) 라우팅에 필요.
     * 토큰 없거나 무효면 익명 통과(공개 시세 피드 유지).
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String auth = accessor.getFirstNativeHeader("Authorization");
                    if (auth != null && auth.startsWith("Bearer ")) {
                        try {
                            TokenClaims claims = jwtTokenProvider.parse(auth.substring(7));
                            accessor.setUser(new StompPrincipal(claims.userId()));
                        } catch (Exception e) {
                            log.debug("STOMP CONNECT 토큰 무효 — 익명 진행: {}", e.toString());
                        }
                    }
                }
                return message;
            }
        });
    }

    /** STOMP 세션 Principal — name = userId. convertAndSendToUser 라우팅 키. */
    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }

    @Bean
    public ThreadPoolTaskScheduler stompHeartbeatScheduler() {
        ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
        s.setPoolSize(1);
        s.setThreadNamePrefix("stomp-heartbeat-");
        s.initialize();
        return s;
    }
}
