package com.fintech.simulator.market.provider.stooq;

import com.fintech.simulator.market.dto.DailyCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Stooq 일봉 CSV 다운로드 클라이언트 (미국 종목용, 무료·키 불필요).
 *
 * <h3>왜 Stooq 인가</h3>
 * Finnhub 무료 티어는 {@code /stock/candle}(일봉) 을 막아둠 → 미국 종목 과거 일봉을
 * 받을 무료 경로가 없었다. Stooq 는 키 없이 CSV 로 1년+ 일봉을 내려준다.
 *
 * <h3>엔드포인트</h3>
 * <pre>GET https://stooq.com/q/d/l/?s=aapl.us&d1=20250528&d2=20260528&i=d</pre>
 * 미국 심볼은 소문자 + {@code .us} 접미사. 응답은 CSV (최신순 아님, 과거→최신):
 * <pre>Date,Open,High,Low,Close,Volume
 * 2025-05-28,189.51,191.00,188.70,190.30,42150000</pre>
 *
 * <h3>제약</h3>
 * 1년치(~252거래일)가 한 응답에 들어와 페이징 불필요. Stooq 무료 CSV 일일 다운로드
 * 한도가 있으나 30종목/일 수준은 안전. User-Agent 없으면 차단될 수 있어 명시.
 */
@Slf4j
@Component
public class StooqDailyCandleClient {

    private static final String BASE_URL = "https://stooq.com";
    private static final DateTimeFormatter REQ_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter CSV_FMT = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    private final RestClient restClient = RestClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.USER_AGENT, "mockvibe/1.0 (daily-candle-backfill)")
            .build();

    /**
     * 한 종목의 [from, to] 구간 일봉 전체를 CSV 한 번에 받아 파싱.
     *
     * @return 과거→최신 순 OHLCV. 실패·데이터 없음 시 빈 리스트.
     */
    public List<DailyCandle> fetch(String ticker, LocalDate from, LocalDate to) {
        String symbol = ticker.toLowerCase() + ".us";
        String csv;
        try {
            csv = restClient.get()
                    .uri(uri -> uri.path("/q/d/l/")
                            .queryParam("s", symbol)
                            .queryParam("d1", from.format(REQ_FMT))
                            .queryParam("d2", to.format(REQ_FMT))
                            .queryParam("i", "d")
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("Stooq fetch failed for {} ({}): {}", ticker, symbol, e.toString());
            return List.of();
        }
        return parse(ticker, csv);
    }

    private List<DailyCandle> parse(String ticker, String csv) {
        if (csv == null || csv.isBlank()) return List.of();

        List<DailyCandle> out = new ArrayList<>();
        String[] lines = csv.split("\\R");
        for (String line : lines) {
            // 헤더 / 빈 줄 / "No data" 류 스킵
            if (line.isBlank() || line.startsWith("Date") || !Character.isDigit(line.charAt(0))) {
                continue;
            }
            String[] f = line.split(",");
            if (f.length < 5) continue;
            try {
                LocalDate date = LocalDate.parse(f[0], CSV_FMT);
                BigDecimal o = parseNum(f[1]);
                BigDecimal h = parseNum(f[2]);
                BigDecimal l = parseNum(f[3]);
                BigDecimal c = parseNum(f[4]);
                if (o == null || h == null || l == null || c == null) continue;  // N/D 등
                long vol = (f.length >= 6) ? parseVol(f[5]) : 0L;
                out.add(new DailyCandle(date, o, h, l, c, vol));
            } catch (Exception e) {
                log.debug("Bad Stooq row for {}: '{}' — skip", ticker, line);
            }
        }
        return out;
    }

    private BigDecimal parseNum(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || "N/D".equalsIgnoreCase(s)) return null;
        return new BigDecimal(s);
    }

    private long parseVol(String s) {
        if (s == null) return 0L;
        s = s.trim();
        if (s.isEmpty() || "N/D".equalsIgnoreCase(s)) return 0L;
        return Long.parseLong(s);
    }
}
