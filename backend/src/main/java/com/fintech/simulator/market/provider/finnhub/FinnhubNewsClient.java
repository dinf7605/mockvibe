package com.fintech.simulator.market.provider.finnhub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintech.simulator.market.dto.NewsItem;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Finnhub `/company-news` REST 호출 — 미국 종목 뉴스 (무료 티어 지원).
 *
 * <pre>GET /company-news?symbol=AAPL&from=2024-01-01&to=2024-01-07&token={apiKey}</pre>
 * - 미국 심볼(알파벳)만 지원. KRX(숫자 티커)는 호출자가 빈 리스트로 처리.
 * - 응답 다수 → 최신순 정렬 후 상한 개수만 반환.
 * - app.external.finnhub.api-key 없으면 Bean 비활성.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.external.finnhub.api-key")
public class FinnhubNewsClient {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MAX_ITEMS = 15;

    private final FinnhubProperties props;
    private final FinnhubRateLimiter rateLimiter;
    private final RestClient restClient;

    public FinnhubNewsClient(FinnhubProperties props, FinnhubRateLimiter rateLimiter) {
        this.props = props;
        this.rateLimiter = rateLimiter;
        this.restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    @CircuitBreaker(name = "fx-rate", fallbackMethod = "fallbackEmpty")
    public List<NewsItem> companyNews(String ticker, int days) {
        rateLimiter.acquire();
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(1, days));

        FinnhubNews[] res = restClient.get()
                .uri(uri -> uri.path("/company-news")
                        .queryParam("symbol", ticker)
                        .queryParam("from", from.format(FMT))
                        .queryParam("to", to.format(FMT))
                        .queryParam("token", props.apiKey())
                        .build())
                .retrieve()
                .body(FinnhubNews[].class);

        if (res == null || res.length == 0) return List.of();
        return java.util.Arrays.stream(res)
                .filter(n -> n.headline() != null && !n.headline().isBlank())
                .sorted(Comparator.comparingLong(FinnhubNews::datetime).reversed())
                .limit(MAX_ITEMS)
                .map(n -> new NewsItem(n.headline(), n.source(), n.summary(), n.url(), n.datetime(), n.image()))
                .toList();
    }

    @SuppressWarnings("unused")
    private List<NewsItem> fallbackEmpty(String ticker, int days, Throwable t) {
        log.warn("Finnhub news failed for {}: {}", ticker, t.toString());
        return List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinnhubNews(
            String headline,
            String source,
            String summary,
            String url,
            String image,
            @JsonProperty("datetime") long datetime
    ) {}
}
