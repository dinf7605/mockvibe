package com.fintech.simulator.fx;

import com.fintech.simulator.fx.domain.FxRate;
import com.fintech.simulator.fx.repository.FxRateRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExchangeRate-API 실연동 환율 제공자.
 *
 *  - 1분 메모리 캐시 + (cacheTtlSeconds) TTL
 *  - 캐시 미스 시 GET /latest/USD → 전체 통화 환율 한 번에 갱신
 *  - 갱신마다 FX_RATES 시계열 INSERT (PRD §8 FX_RATES)
 *  - API 오류 시 마지막 캐시값으로 폴백 → 매매가 멈추지 않음
 *
 * Bean 자체는 항상 등록 — StubFxRateProvider는 ConditionalOnMissingBean으로 자동 비활성.
 */
@Slf4j
@Component
public class ApiFxRateProvider implements FxRateProvider {

    private static final String BASE = "USD";
    /** API 미응답·미설정 시 사용할 USD→KRW 안전망 (StubFxRateProvider와 동일) */
    private static final BigDecimal FALLBACK_USD_KRW = new BigDecimal("1380.0");

    private final FxProperties props;
    private final FxRateRepository fxRateRepository;
    private final RestClient restClient;

    private final ConcurrentHashMap<String, BigDecimal> ratesFromUsd = new ConcurrentHashMap<>();
    private volatile Instant cachedAt = Instant.EPOCH;

    public ApiFxRateProvider(FxProperties props, FxRateRepository fxRateRepository) {
        this.props = props;
        this.fxRateRepository = fxRateRepository;
        this.restClient = (props.baseUrl() == null || props.baseUrl().isBlank())
                ? null
                : RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    @Override
    public BigDecimal rate(String base, String quote) {
        if (base == null || quote == null || base.equals(quote)) return BigDecimal.ONE;

        refreshIfStale();

        if (BASE.equals(base)) {
            return ratesFromUsd.getOrDefault(quote, fallback(BASE, quote));
        }
        // 비-USD base는 USD를 거쳐 환산: base→USD→quote
        BigDecimal baseToUsd = ratesFromUsd.containsKey(base)
                ? BigDecimal.ONE.divide(ratesFromUsd.get(base), 6, RoundingMode.HALF_UP)
                : BigDecimal.ONE;
        BigDecimal usdToQuote = ratesFromUsd.getOrDefault(quote, fallback(BASE, quote));
        return baseToUsd.multiply(usdToQuote).setScale(6, RoundingMode.HALF_UP);
    }

    /** 매분 강제 갱신 (DB 시계열 적재 포함). 캐시 hit 여부와 무관. */
    @Scheduled(fixedDelayString = "${app.external.fx.refresh-interval-seconds:60}000")
    public void scheduledRefresh() {
        try {
            doFetch();
        } catch (RestClientException e) {
            log.warn("FX scheduled refresh failed: {}", e.getMessage());
        }
    }

    private void refreshIfStale() {
        if (Instant.now().isBefore(cachedAt.plusSeconds(props.cacheTtlSeconds()))) return;
        try {
            doFetch();
        } catch (RestClientException e) {
            log.warn("FX lazy refresh failed, keep stale cache: {}", e.getMessage());
        }
    }

    @CircuitBreaker(name = "fx-rate", fallbackMethod = "doFetchFallback")
    @Transactional
    public synchronized void doFetch() {
        if (restClient == null) return;
        FxResponse res = restClient.get().uri("/latest/" + BASE).retrieve().body(FxResponse.class);
        if (res == null || res.rates() == null) return;

        // 메모리 캐시 갱신
        res.rates().forEach((quote, rate) ->
                ratesFromUsd.put(quote, rate.setScale(6, RoundingMode.HALF_UP)));
        cachedAt = Instant.now();

        // 핵심 페어만 DB 시계열 적재 (전체 통화 적재는 디스크 낭비)
        BigDecimal krw = res.rates().get("KRW");
        if (krw != null) {
            fxRateRepository.save(FxRate.snapshot(BASE, "KRW", krw));
            log.debug("FX_RATES snapshot: USD→KRW={}", krw);
        }
    }

    /** CB Open / 호출 실패 시 fallback. 매매 멈춤 방지 — 기존 캐시값 유지하고 silent return. */
    @SuppressWarnings("unused")
    public void doFetchFallback(Throwable t) {
        log.warn("FX fetch CB fallback (keep stale cache): {}", t.getMessage());
    }

    private BigDecimal fallback(String base, String quote) {
        if ("USD".equals(base) && "KRW".equals(quote)) return FALLBACK_USD_KRW;
        return BigDecimal.ONE;
    }

    public record FxResponse(String base, Map<String, BigDecimal> rates) {}
}
