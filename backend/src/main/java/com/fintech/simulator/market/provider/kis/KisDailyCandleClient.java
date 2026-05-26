package com.fintech.simulator.market.provider.kis;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * KIS 일봉 시계열 호출. 한 종목 × 한 페이지(최대 100거래일).
 * - 1년치(~252거래일)는 3 페이지 호출 필요. 호출자 측에서 페이지 슬라이딩.
 * - Bucket4j(KisRateLimiter) 가 KisRestClient.get() 내부에서 자동 throttling.
 * - 외부 실패 시 Resilience4j CB(kis-rest) 가 회로 차단.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisDailyCandleClient {

    private static final String TR_ID = "FHKST03010100";
    private static final String PATH  = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KisRestClient restClient;

    /**
     * 한 종목의 [fromDate, toDate] 구간 일봉을 한 페이지(최대 100건)로 받아온다.
     * 100건 초과 구간은 호출자가 toDate 를 앞으로 미루며 재호출.
     *
     * @return 최신 → 과거 순서의 일봉 리스트 (API 원본 순서 그대로). 실패 시 빈 리스트.
     */
    @CircuitBreaker(name = "kis-rest", fallbackMethod = "fallbackEmpty")
    public List<KisDailyChartResponse.KisDailyCandleItem> fetch(
            String ticker, LocalDate fromDate, LocalDate toDate) {

        String query = String.format(
                "%s?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=%s" +
                "&FID_INPUT_DATE_1=%s&FID_INPUT_DATE_2=%s" +
                "&FID_PERIOD_DIV_CODE=D&FID_ORG_ADJ_PRC=0",
                PATH, ticker, fromDate.format(FMT), toDate.format(FMT));

        KisDailyChartResponse resp = restClient.get(query, TR_ID, KisDailyChartResponse.class);

        if (resp == null || !resp.isSuccess()) {
            log.warn("KIS daily candle non-success for {} [{}~{}]: rt_cd={} msg={}",
                    ticker, fromDate, toDate,
                    resp == null ? "null" : resp.rtCd(),
                    resp == null ? "null" : resp.msg1());
            return Collections.emptyList();
        }
        List<KisDailyChartResponse.KisDailyCandleItem> items = resp.output2();
        return items == null ? Collections.emptyList() : items;
    }

    @SuppressWarnings("unused")
    private List<KisDailyChartResponse.KisDailyCandleItem> fallbackEmpty(
            String ticker, LocalDate fromDate, LocalDate toDate, Throwable t) {
        log.warn("CB open or call failed for KIS daily candle {} [{}~{}]: {}",
                ticker, fromDate, toDate, t.toString());
        return Collections.emptyList();
    }
}
