package com.fintech.simulator.market.provider.kis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintech.simulator.market.provider.Quote;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * KIS 현재가 조회 (inquire-price).
 *
 * <pre>
 * GET /uapi/domestic-stock/v1/quotations/inquire-price
 *   tr_id=FHKST01010100
 *   FID_COND_MRKT_DIV_CODE=J
 *   FID_INPUT_ISCD=005930
 * </pre>
 * 응답: output.stck_prpr(현재가) output.stck_sdpr(전일종가=기준가)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisQuoteClient {

    private static final String TR_ID = "FHKST01010100";
    private static final String PATH  = "/uapi/domestic-stock/v1/quotations/inquire-price";

    private final KisRestClient restClient;

    @CircuitBreaker(name = "kis-rest", fallbackMethod = "fallbackEmpty")
    public Optional<Quote> quote(String ticker) {
        String query = PATH + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + ticker;
        KisPriceResponse r = restClient.get(query, TR_ID, KisPriceResponse.class);

        if (r == null || !r.isSuccess() || r.output() == null
                || r.output().currentPrice() == null || r.output().currentPrice().isBlank()) {
            return Optional.empty();
        }
        try {
            BigDecimal price = new BigDecimal(r.output().currentPrice());
            if (price.signum() <= 0) return Optional.empty();
            BigDecimal prevClose = r.output().prevClose() != null && !r.output().prevClose().isBlank()
                    ? new BigDecimal(r.output().prevClose()) : null;
            return Optional.of(new Quote(ticker, price, prevClose, Instant.now()));
        } catch (NumberFormatException e) {
            log.warn("KIS quote parse fail for {}: {}", ticker, e.toString());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused")
    private Optional<Quote> fallbackEmpty(String ticker, Throwable t) {
        log.warn("KIS quote failed for {}: {}", ticker, t.toString());
        return Optional.empty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KisPriceResponse(
            @JsonProperty("rt_cd") String rtCd,
            @JsonProperty("output") Output output
    ) {
        public boolean isSuccess() { return "0".equals(rtCd); }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Output(
                @JsonProperty("stck_prpr") String currentPrice,   // 현재가
                @JsonProperty("stck_sdpr") String prevClose        // 전일종가(기준가)
        ) {}
    }
}
