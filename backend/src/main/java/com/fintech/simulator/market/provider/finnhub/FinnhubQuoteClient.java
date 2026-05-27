package com.fintech.simulator.market.provider.finnhub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Finnhub `/quote` REST 호출 (무료 티어 지원).
 *
 * <pre>GET /quote?symbol=AAPL&token={apiKey}</pre>
 * 응답: c(current) d(change) dp(changePct) h(high) l(low) o(open) pc(prevClose) t(unix sec)
 *
 * - 무료 티어는 일봉(/stock/candle) 미지원 → 본 quote 로 현재가만 수집
 * - 장 외 시간엔 c 가 직전 종가로 유지됨 (Finnhub 특성)
 * - FinnhubRateLimiter (분당 60) 로 throttle, CB(name="fx-rate" 재사용 대신 별도 없음 → kis-rest 와 분리 위해 finnhub 전용은 추후)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.external.finnhub.api-key")
public class FinnhubQuoteClient {

    private final FinnhubProperties props;
    private final FinnhubRateLimiter rateLimiter;
    private final RestClient restClient;

    public FinnhubQuoteClient(FinnhubProperties props, FinnhubRateLimiter rateLimiter) {
        this.props = props;
        this.rateLimiter = rateLimiter;
        this.restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    @CircuitBreaker(name = "fx-rate", fallbackMethod = "fallbackEmpty")
    public Optional<com.fintech.simulator.market.provider.Quote> quote(String ticker) {
        rateLimiter.acquire();
        FinnhubQuoteResponse r = restClient.get()
                .uri(uri -> uri.path("/quote")
                        .queryParam("symbol", ticker)
                        .queryParam("token", props.apiKey())
                        .build())
                .retrieve()
                .body(FinnhubQuoteResponse.class);

        if (r == null || r.current() == null || r.current().signum() <= 0) {
            return Optional.empty();   // 데이터 없음 (장 시작 전/심볼 오류)
        }
        Instant ts = r.timestamp() != null && r.timestamp() > 0
                ? Instant.ofEpochSecond(r.timestamp()) : Instant.now();
        return Optional.of(new com.fintech.simulator.market.provider.Quote(
                ticker, r.current(), r.prevClose(), ts));
    }

    @SuppressWarnings("unused")
    private Optional<com.fintech.simulator.market.provider.Quote> fallbackEmpty(String ticker, Throwable t) {
        log.warn("Finnhub quote failed for {}: {}", ticker, t.toString());
        return Optional.empty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinnhubQuoteResponse(
            @JsonProperty("c")  BigDecimal current,
            @JsonProperty("pc") BigDecimal prevClose,
            @JsonProperty("h")  BigDecimal high,
            @JsonProperty("l")  BigDecimal low,
            @JsonProperty("o")  BigDecimal open,
            @JsonProperty("t")  Long timestamp
    ) {}
}
