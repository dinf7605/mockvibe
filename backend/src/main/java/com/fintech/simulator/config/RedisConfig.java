package com.fintech.simulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * RedisTemplate 단순 문자열 전용.
 * 키 규칙:
 *   - RT:{userId}        → Refresh Token 값 (TTL = refresh validity)
 *   - BL:AT:{jti}        → "1" (Access 블랙리스트, TTL = 남은 만료시간)
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
