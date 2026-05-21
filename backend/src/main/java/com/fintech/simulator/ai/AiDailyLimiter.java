package com.fintech.simulator.ai;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 사용자당 AI 코치 호출 일일 한도 (PRD FR-7.1).
 *
 * 키: AI:CALL:{userId}:{YYYY-MM-DD}, INCR + EXPIRE 25h.
 * - acquire(): 한도 미달이면 카운트 +1, 초과면 AI_DAILY_LIMIT 예외
 * - peek(): 현재 카운트만 조회 (관리자 화면용, D44)
 */
@Component
@RequiredArgsConstructor
public class AiDailyLimiter {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final StringRedisTemplate redis;
    private final GeminiProperties props;

    public void acquire(String userId) {
        String key = key(userId);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofHours(25));
        }
        if (count != null && count > props.dailyCallLimitPerUser()) {
            throw new BusinessException(ErrorCode.AI_DAILY_LIMIT,
                    "오늘 " + props.dailyCallLimitPerUser() + "회 한도 도달");
        }
    }

    public long peek(String userId) {
        String v = redis.opsForValue().get(key(userId));
        return v == null ? 0 : Long.parseLong(v);
    }

    private String key(String userId) {
        return "AI:CALL:" + userId + ":" + LocalDate.now(ZONE);
    }
}
