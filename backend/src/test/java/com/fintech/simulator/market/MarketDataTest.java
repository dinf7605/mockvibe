package com.fintech.simulator.market;

import com.fintech.simulator.market.cache.PriceCache;
import com.fintech.simulator.market.controller.PriceResponse;
import com.fintech.simulator.market.event.PriceUpdatedEvent;
import com.fintech.simulator.market.provider.MockMarketDataProvider;
import com.fintech.simulator.market.provider.Quote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Market 도메인 가벼운 단위 테스트.
 * Provider 인터페이스 / 캐시 / 변환 로직만 검증. 스케줄러는 통합 테스트(D10+)에서.
 */
class MarketDataTest {

    @Test
    @DisplayName("PriceCache put/get/clear")
    void price_cache_ops() {
        PriceCache cache = new PriceCache();
        Quote q = new Quote("AAPL", new BigDecimal("225.50"), new BigDecimal("224.00"), Instant.now());

        cache.put(q);
        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.get("AAPL")).contains(q);
        cache.clear();
        assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("MockProvider: 시드 종목은 supports, tick 후 PriceUpdatedEvent 발행")
    void mock_provider_supports_and_ticks() {
        PriceCache cache = new PriceCache();
        AtomicInteger published = new AtomicInteger();
        ApplicationEventPublisher publisher = e -> {
            if (e instanceof PriceUpdatedEvent) published.incrementAndGet();
        };
        MockMarketDataProvider mock = new MockMarketDataProvider(cache, publisher);

        assertThat(mock.name()).isEqualTo("MOCK");
        assertThat(mock.supports("005930")).isTrue();
        assertThat(mock.supports("UNKNOWN")).isFalse();

        Quote before = mock.getQuote("005930").orElseThrow();
        mock.tick();
        Quote after = mock.getQuote("005930").orElseThrow();

        assertThat(after.timestamp()).isAfterOrEqualTo(before.timestamp());
        // 시드 종목 수만큼 이벤트 발행 (현재 3개)
        assertThat(published.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("PriceResponse 변환: changePct 정확 (상승)")
    void price_response_up() {
        Quote q = new Quote("AAPL", new BigDecimal("110.00"), new BigDecimal("100.00"), Instant.now());
        PriceResponse r = PriceResponse.from(q);
        assertThat(r.changePct()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("PriceResponse 변환: changePct 정확 (하락)")
    void price_response_down() {
        Quote q = new Quote("AAPL", new BigDecimal("95.00"), new BigDecimal("100.00"), Instant.now());
        PriceResponse r = PriceResponse.from(q);
        assertThat(r.changePct()).isEqualByComparingTo("-5.00");
    }

    @Test
    @DisplayName("PriceResponse: prevClose 없으면 changePct=0")
    void price_response_no_prev() {
        Quote q = new Quote("AAPL", new BigDecimal("100.00"), null, Instant.now());
        PriceResponse r = PriceResponse.from(q);
        assertThat(r.changePct()).isEqualByComparingTo("0");
    }
}
