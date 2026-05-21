package com.fintech.simulator.market.provider.kis;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisRateLimiterTest {

    @Test
    @DisplayName("초당 5건까지 허용, 6번째는 EXTERNAL_RATE_LIMITED")
    void five_per_second() {
        KisRateLimiter limiter = new KisRateLimiter();
        for (int i = 0; i < 5; i++) limiter.acquire();
        assertThatThrownBy(limiter::acquire)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_RATE_LIMITED);
    }

    @Test
    @DisplayName("availableTokens 초기값 5")
    void initial_capacity() {
        assertThat(new KisRateLimiter().availableTokens()).isEqualTo(5);
    }
}
