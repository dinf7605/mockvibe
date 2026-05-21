package com.fintech.simulator.market.provider.kis;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import io.github.bucket4j.Bucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * KIS REST 호출 토큰 버킷 (초당 5건, PRD §9.1).
 * - 초과 시 BusinessException(EXTERNAL_RATE_LIMITED) → HTTP 429
 */
@Component
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisRateLimiter {

    private final Bucket bucket;

    public KisRateLimiter() {
        this.bucket = Bucket.builder()
                .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofSeconds(1)))
                .build();
    }

    public void acquire() {
        if (!bucket.tryConsume(1)) {
            throw new BusinessException(ErrorCode.EXTERNAL_RATE_LIMITED, "KIS REST rate limit (5/s)");
        }
    }

    public long availableTokens() {
        return bucket.getAvailableTokens();
    }
}
