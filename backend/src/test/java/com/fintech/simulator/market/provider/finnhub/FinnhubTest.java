package com.fintech.simulator.market.provider.finnhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.provider.Quote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinnhubTest {

    private final FinnhubMessageParser parser = new FinnhubMessageParser(new ObjectMapper());

    @Test
    @DisplayName("Parser: type=trade 다건 data → 모두 Quote 변환")
    void parse_trade_multi() {
        String json = """
                {"type":"trade","data":[
                  {"s":"AAPL","p":225.10,"t":1700000000000,"v":100},
                  {"s":"AAPL","p":225.20,"t":1700000000500,"v":150},
                  {"s":"NVDA","p":145.00,"t":1700000001000,"v":50}
                ]}""";
        List<Quote> quotes = parser.parse(json);
        assertThat(quotes).hasSize(3);
        assertThat(quotes).extracting(Quote::ticker).containsExactly("AAPL", "AAPL", "NVDA");
        assertThat(quotes.get(2).price()).isEqualByComparingTo("145.00");
    }

    @Test
    @DisplayName("Parser: type=ping/empty/invalid → empty 리스트")
    void parse_non_trade() {
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("{\"type\":\"ping\"}")).isEmpty();
        assertThat(parser.parse("not-a-json")).isEmpty();
        // data 필드 누락
        assertThat(parser.parse("{\"type\":\"trade\"}")).isEmpty();
    }

    @Test
    @DisplayName("RateLimiter: 60건까지 OK, 61번째는 EXTERNAL_RATE_LIMITED")
    void rate_limit_60_per_min() {
        FinnhubProperties props = new FinnhubProperties("u", "wss", "k", 60);
        FinnhubRateLimiter limiter = new FinnhubRateLimiter(props);
        for (int i = 0; i < 60; i++) limiter.acquire();
        assertThatThrownBy(limiter::acquire)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_RATE_LIMITED);
    }

    @Test
    @DisplayName("FinnhubProperties: ratePerMinute null → 기본 60")
    void properties_default() {
        assertThat(new FinnhubProperties("u", "wss", "k", null).ratePerMinute()).isEqualTo(60);
    }

    @Test
    @DisplayName("Provider: 영문 1~5자 ticker만 supports")
    void provider_supports() {
        // supports() 는 quote client 를 쓰지 않으므로 null 주입 (Step 2·3 에서 생성자 시그니처 변경)
        FinnhubMarketDataProvider p = new FinnhubMarketDataProvider(null);
        assertThat(p.supports("AAPL")).isTrue();
        assertThat(p.supports("NVDA")).isTrue();
        assertThat(p.supports("005930")).isFalse();
        assertThat(p.supports("TOOLONG")).isFalse();
        assertThat(p.supports(null)).isFalse();
    }
}
