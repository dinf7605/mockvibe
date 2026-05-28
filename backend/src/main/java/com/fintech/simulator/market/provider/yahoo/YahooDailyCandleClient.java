package com.fintech.simulator.market.provider.yahoo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fintech.simulator.market.dto.DailyCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Yahoo Finance 차트 JSON 다운로드 클라이언트 (미국 종목 일봉, 무료·키 불필요).
 *
 * <h3>왜 Yahoo 인가</h3>
 * - Finnhub 무료: 일봉 미지원
 * - Stooq: 2026-05 이후 captcha apikey 필수로 정책 변경
 * - Yahoo {@code /v8/finance/chart/} : 키·캡차 없이 1년치 OHLCV 를 단일 JSON 으로 반환
 *
 * <h3>엔드포인트</h3>
 * <pre>GET https://query1.finance.yahoo.com/v8/finance/chart/AAPL?period1=...&period2=...&interval=1d</pre>
 * {@code period1/period2} 는 unix epoch seconds. 응답 timestamp 는 거래일 9:30 ET (market open).
 * 거래소 타임존(America/New_York) 기준으로 LocalDate 변환.
 *
 * <h3>주의</h3>
 * - User-Agent 미설정 시 401. 일반 브라우저 UA 명시 필수.
 * - JSON 구조: {@code chart.result[0].timestamp[] + chart.result[0].indicators.quote[0].(open|high|low|close|volume)[]}.
 *   각 배열 동일 길이. null 원소(공휴일/halted)는 스킵.
 */
@Slf4j
@Component
public class YahooDailyCandleClient {

    private static final String BASE_URL = "https://query1.finance.yahoo.com";
    private static final ZoneId NYSE_ZONE = ZoneId.of("America/New_York");
    /** Yahoo 가 차단 안 하도록 일반 브라우저 UA 모방 */
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final RestClient restClient = RestClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.USER_AGENT, UA)
            .defaultHeader(HttpHeaders.ACCEPT, "application/json")
            .build();

    /**
     * 한 종목의 [from, to] 구간 일봉을 JSON 한 번에 받아 파싱.
     *
     * @return 과거→최신 순 OHLCV. 실패·데이터 없음 시 빈 리스트.
     */
    public List<DailyCandle> fetch(String ticker, LocalDate from, LocalDate to) {
        // Yahoo period 는 inclusive 경계가 미묘 — to 는 +1일 해서 마지막 날 포함을 보장
        long p1 = from.atStartOfDay(NYSE_ZONE).toEpochSecond();
        long p2 = to.plusDays(1).atStartOfDay(NYSE_ZONE).toEpochSecond();

        YahooChartResponse resp;
        try {
            resp = restClient.get()
                    .uri(uri -> uri.path("/v8/finance/chart/{ticker}")
                            .queryParam("period1", p1)
                            .queryParam("period2", p2)
                            .queryParam("interval", "1d")
                            .build(ticker))
                    .retrieve()
                    .body(YahooChartResponse.class);
        } catch (Exception e) {
            log.warn("Yahoo fetch failed for {}: {}", ticker, e.toString());
            return List.of();
        }
        return parse(ticker, resp);
    }

    private List<DailyCandle> parse(String ticker, YahooChartResponse resp) {
        if (resp == null || resp.chart() == null) return List.of();
        if (resp.chart().error() != null) {
            log.warn("Yahoo returned error for {}: {}", ticker, resp.chart().error());
            return List.of();
        }
        List<YahooResult> results = resp.chart().result();
        if (results == null || results.isEmpty()) return List.of();
        YahooResult r = results.get(0);
        if (r.timestamp() == null || r.indicators() == null
                || r.indicators().quote() == null || r.indicators().quote().isEmpty()) {
            return List.of();
        }

        List<Long> ts = r.timestamp();
        YahooQuote q = r.indicators().quote().get(0);
        int n = ts.size();
        List<DailyCandle> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BigDecimal open  = pick(q.open(), i);
            BigDecimal high  = pick(q.high(), i);
            BigDecimal low   = pick(q.low(), i);
            BigDecimal close = pick(q.close(), i);
            if (open == null || high == null || low == null || close == null) continue;

            LocalDate date = Instant.ofEpochSecond(ts.get(i)).atZone(NYSE_ZONE).toLocalDate();
            long vol = (q.volume() != null && i < q.volume().size() && q.volume().get(i) != null)
                    ? q.volume().get(i) : 0L;
            out.add(new DailyCandle(date,
                    open.setScale(4, RoundingMode.HALF_UP),
                    high.setScale(4, RoundingMode.HALF_UP),
                    low.setScale(4, RoundingMode.HALF_UP),
                    close.setScale(4, RoundingMode.HALF_UP),
                    vol));
        }
        return out;
    }

    private BigDecimal pick(List<Double> list, int i) {
        if (list == null || i >= list.size()) return null;
        Double v = list.get(i);
        if (v == null || v.isNaN() || v.isInfinite()) return null;
        return BigDecimal.valueOf(v);
    }

    // ===== JSON DTO =====
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooChartResponse(YahooChart chart) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooChart(List<YahooResult> result, Object error) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooResult(List<Long> timestamp, YahooIndicators indicators) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooIndicators(List<YahooQuote> quote) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record YahooQuote(
            List<Double> open,
            List<Double> high,
            List<Double> low,
            List<Double> close,
            List<Long> volume
    ) {}
}
