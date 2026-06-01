package com.fintech.simulator.market.provider.finnhub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.simulator.market.dto.NewsItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Finnhub `/company-news` REST 호출 — 미국 종목 뉴스 (무료 티어).
 *
 * <h3>회복력 (company-news 는 응답이 크고 느림: AAPL 7일 ≈ 245건/11s, 종종 504)</h3>
 * <ul>
 *   <li><b>connect 3s / read 8s 타임아웃</b> — 느린 응답은 빠르게 포기하고 빈 목록</li>
 *   <li><b>Redis 캐시 15분</b> — 한 번 받으면 재호출 없이 즉시 반환</li>
 *   <li>모든 예외는 try/catch 로 흡수 → 504/500 전파 방지, 카드 숨김(빈 목록)</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.external.finnhub.api-key")
public class FinnhubNewsClient {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MAX_ITEMS = 15;
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final TypeReference<List<NewsItem>> LIST_TYPE = new TypeReference<>() {};

    private final FinnhubProperties props;
    private final FinnhubRateLimiter rateLimiter;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public FinnhubNewsClient(FinnhubProperties props, FinnhubRateLimiter rateLimiter,
                             StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.props = props;
        this.rateLimiter = rateLimiter;
        this.redis = redis;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(3_000);
        rf.setReadTimeout(10_000);
        this.restClient = RestClient.builder().baseUrl(props.baseUrl()).requestFactory(rf).build();
    }

    /**
     * 캐시 우선 → 미스 시 Finnhub 호출(타임아웃 보호). 어떤 예외도 빈 목록으로 흡수.
     * (자가호출이라 CB 애노테이션 대신 직접 try/catch — 타임아웃이 핵심 방어)
     */
    public List<NewsItem> companyNews(String ticker, int days) {
        String cacheKey = "news:" + ticker;
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) return objectMapper.readValue(cached, LIST_TYPE);
        } catch (Exception e) {
            log.debug("news cache 조회 실패 {}: {}", ticker, e.toString());
        }

        List<NewsItem> items;
        try {
            items = fetch(ticker, days);
        } catch (Exception e) {
            log.warn("Finnhub news failed for {}: {}", ticker, e.toString());
            return List.of();
        }

        if (!items.isEmpty()) {
            try {
                redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(items), CACHE_TTL);
            } catch (Exception e) {
                log.debug("news cache 저장 실패 {}: {}", ticker, e.toString());
            }
        }
        return items;
    }

    private List<NewsItem> fetch(String ticker, int days) {
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
