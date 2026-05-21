package com.fintech.simulator.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

import java.util.List;

/**
 * STOMP 메시지 브로커 설정.
 *
 *  - 엔드포인트: ws://host/ws (SockJS X — 순수 WebSocket)
 *  - 토픽: /topic/price/{ticker} → 종목별 시세 push (PriceBroadcaster)
 *  - Heartbeat: 클라/서버 15초 (PRD FR-5.5)
 *  - Origin: CorsProperties와 동일한 정책
 *
 * 인증: D11에서는 공개 채널. JWT 통합은 후속 Phase에서 ChannelInterceptor로 추가.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final long HEARTBEAT_MS = 15_000L;

    private final CorsProperties corsProperties;

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

    @Bean
    public ThreadPoolTaskScheduler stompHeartbeatScheduler() {
        ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
        s.setPoolSize(1);
        s.setThreadNamePrefix("stomp-heartbeat-");
        s.initialize();
        return s;
    }
}
