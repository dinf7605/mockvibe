package com.fintech.simulator.market.provider.finnhub;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import io.github.bucket4j.Bucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Finnhub REST 호출 토큰 버킷 (분당 60건, PRD §9.2 무료 티어).
 * 초과 시 BusinessException(EXTERNAL_RATE_LIMITED) → HTTP 429.
 */
@Component
@ConditionalOnProperty(name = "app.external.finnhub.api-key")
public class FinnhubRateLimiter {

    private final Bucket bucket;

    public FinnhubRateLimiter(FinnhubProperties props) {
        int perMin = props.ratePerMinute();
        this.bucket = Bucket.builder()
                .addLimit(limit -> limit.capacity(perMin).refillGreedy(perMin, Duration.ofMinutes(1)))
                .build();
    }

    public void acquire() {
        if (!bucket.tryConsume(1)) {
            throw new BusinessException(ErrorCode.EXTERNAL_RATE_LIMITED, "Finnhub rate limit (60/min)");
        }
    }

    public long availableTokens() { return bucket.getAvailableTokens(); }
}
