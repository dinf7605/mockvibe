package com.fintech.simulator.market.seed;

import com.fintech.simulator.market.domain.PriceHistory;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.repository.PriceHistoryRepository;
import com.fintech.simulator.market.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1년치 OHLC 시드 (Mock 랜덤워크). 외부 API 키 없이도 D27~D31이 동작하도록 보장.
 * KIS/Finnhub 실연동은 후속 Phase에서 동일 테이블에 INSERT/UPSERT.
 *
 * <h3>Base price 정책</h3>
 * <ol>
 *   <li>{@link #BASE_PRICES} 매핑 (실제 시세 근사치, 2026-05 기준)</li>
 *   <li>{@code STOCKS.current_price} (이전에 채워진 값 있다면 그대로)</li>
 *   <li>기본값 (USD 100 / KRW 50,000)</li>
 * </ol>
 *
 * <h3>Idempotent</h3>
 * 종목별 PRICE_HISTORY 가 이미 있으면 skip. 재시드 시 ADB 에서
 * {@code TRUNCATE TABLE PRICE_HISTORY} 후 부팅 자동 재시드.
 *
 * <h3>STOCKS.current_price 동기화</h3>
 * 시드 직후 마지막 close 가격을 {@code STOCKS.current_price} 에 UPDATE.
 * → 종목 검색/상세 페이지의 currentPrice 도 자동으로 현실적인 값을 반환.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceHistorySeeder implements ApplicationRunner {

    private static final int DAYS = 365;

    /**
     * 종목별 시드 시작 가격 (실제 시세 근사치, 2026-05 기준).
     * 매핑 없는 종목은 currency 기본값 사용.
     */
    private static final Map<String, Double> BASE_PRICES = new HashMap<>();
    static {
        // ===== 한국 30종목 (KRX) =====
        BASE_PRICES.put("005930",  80_000.0);   // 삼성전자
        BASE_PRICES.put("000660", 200_000.0);   // SK하이닉스
        BASE_PRICES.put("373220", 400_000.0);   // LG에너지솔루션
        BASE_PRICES.put("207940", 900_000.0);   // 삼성바이오로직스
        BASE_PRICES.put("005380", 250_000.0);   // 현대차
        BASE_PRICES.put("000270", 110_000.0);   // 기아
        BASE_PRICES.put("035420", 210_000.0);   // NAVER
        BASE_PRICES.put("035720",  50_000.0);   // 카카오
        BASE_PRICES.put("068270", 190_000.0);   // 셀트리온
        BASE_PRICES.put("005490", 400_000.0);   // POSCO홀딩스
        BASE_PRICES.put("105560",  90_000.0);   // KB금융
        BASE_PRICES.put("055550",  60_000.0);   // 신한지주
        BASE_PRICES.put("086790",  70_000.0);   // 하나금융지주
        BASE_PRICES.put("028260", 150_000.0);   // 삼성물산
        BASE_PRICES.put("012330", 280_000.0);   // 현대모비스
        BASE_PRICES.put("015760",  25_000.0);   // 한국전력공사
        BASE_PRICES.put("003550",  85_000.0);   // LG
        BASE_PRICES.put("017670",  55_000.0);   // SK텔레콤
        BASE_PRICES.put("030200",  45_000.0);   // KT
        BASE_PRICES.put("051910", 340_000.0);   // LG화학
        BASE_PRICES.put("006400", 340_000.0);   // 삼성SDI
        BASE_PRICES.put("096770", 120_000.0);   // SK이노베이션
        BASE_PRICES.put("033780", 110_000.0);   // KT&G
        BASE_PRICES.put("009150", 140_000.0);   // 삼성전기
        BASE_PRICES.put("010130", 600_000.0);   // 고려아연
        BASE_PRICES.put("011200",  20_000.0);   // HMM
        BASE_PRICES.put("010950",  75_000.0);   // S-Oil
        BASE_PRICES.put("251270",  50_000.0);   // 넷마블
        BASE_PRICES.put("036570", 200_000.0);   // 엔씨소프트
        BASE_PRICES.put("066570",  85_000.0);   // LG전자

        // ===== 미국 30종목 (NASDAQ/NYSE, USD) =====
        BASE_PRICES.put("AAPL",   230.0);   // Apple
        BASE_PRICES.put("MSFT",   440.0);   // Microsoft
        BASE_PRICES.put("NVDA",   140.0);   // NVIDIA
        BASE_PRICES.put("AMZN",   210.0);   // Amazon
        BASE_PRICES.put("GOOGL",  170.0);   // Alphabet
        BASE_PRICES.put("META",   580.0);   // Meta
        BASE_PRICES.put("TSLA",   330.0);   // Tesla
        BASE_PRICES.put("LLY",    840.0);   // Eli Lilly
        BASE_PRICES.put("JPM",    230.0);   // JPMorgan
        BASE_PRICES.put("V",      310.0);   // Visa
        BASE_PRICES.put("UNH",    570.0);   // UnitedHealth
        BASE_PRICES.put("JNJ",    155.0);   // Johnson & Johnson
        BASE_PRICES.put("MA",     510.0);   // Mastercard
        BASE_PRICES.put("XOM",    115.0);   // Exxon Mobil
        BASE_PRICES.put("PG",     170.0);   // Procter & Gamble
        BASE_PRICES.put("AVGO",   210.0);   // Broadcom
        BASE_PRICES.put("HD",     395.0);   // Home Depot
        BASE_PRICES.put("COST",   975.0);   // Costco
        BASE_PRICES.put("MRK",     95.0);   // Merck
        BASE_PRICES.put("ABBV",   190.0);   // AbbVie
        BASE_PRICES.put("WMT",     95.0);   // Walmart
        BASE_PRICES.put("KO",      64.0);   // Coca-Cola
        BASE_PRICES.put("PEP",    155.0);   // PepsiCo
        BASE_PRICES.put("DIS",    115.0);   // Disney
        BASE_PRICES.put("NFLX",   870.0);   // Netflix
        BASE_PRICES.put("AMD",    140.0);   // AMD
        BASE_PRICES.put("INTC",    24.0);   // Intel
        BASE_PRICES.put("CSCO",    58.0);   // Cisco
        BASE_PRICES.put("ADBE",   485.0);   // Adobe
        BASE_PRICES.put("CRM",    325.0);   // Salesforce
    }

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Stock> stocks = stockRepository.findAll();
        int seeded = 0;
        int synced = 0;
        for (Stock s : stocks) {
            if (priceHistoryRepository.countByTicker(s.getTicker()) == 0) {
                BigDecimal lastClose = generate(s);
                // 시드 직후 STOCKS.current_price 동기화 — 검색/상세에서도 같은 값 노출
                s.setCurrentPrice(lastClose);
                seeded++;
            } else {
                // 이미 시드된 종목이지만 STOCKS.current_price 가 null 이면 sync 필요
                if (s.getCurrentPrice() == null) {
                    priceHistoryRepository.findTopByTickerOrderByTradeDateDesc(s.getTicker())
                            .ifPresent(h -> s.setCurrentPrice(h.getClose()));
                    synced++;
                }
            }
        }
        if (seeded > 0) log.info("PRICE_HISTORY seeded: {} tickers × ~{} days", seeded, DAYS);
        if (synced > 0) log.info("STOCKS.current_price synced from latest close: {} tickers", synced);
    }

    /** @return 시드의 마지막 close 가격 (STOCKS.current_price 동기화용) */
    private BigDecimal generate(Stock stock) {
        Random rng = new Random(stock.getTicker().hashCode());
        double basePrice = resolveBasePrice(stock);

        LocalDate today = LocalDate.now();
        List<PriceHistory> batch = new ArrayList<>();
        double price = basePrice;
        for (int i = DAYS; i >= 1; i--) {
            LocalDate d = today.minusDays(i);
            DayOfWeek dow = d.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;

            double dailyReturn = rng.nextGaussian() * 0.015;       // 일평균 0%, 표준편차 1.5%
            double prev = price;
            price = Math.max(prev * (1 + dailyReturn), 0.01);
            double high = Math.max(prev, price) * (1 + Math.abs(rng.nextGaussian()) * 0.003);
            double low  = Math.min(prev, price) * (1 - Math.abs(rng.nextGaussian()) * 0.003);
            long volume = 100_000 + (long)(rng.nextDouble() * 9_900_000);

            batch.add(PriceHistory.of(stock.getTicker(), d,
                    bd(prev), bd(high), bd(low), bd(price), volume));
        }
        priceHistoryRepository.saveAll(batch);
        return bd(price);   // 시드 끝점 = 가장 최근 종가
    }

    private double resolveBasePrice(Stock stock) {
        Double mapped = BASE_PRICES.get(stock.getTicker());
        if (mapped != null) return mapped;
        if (stock.getCurrentPrice() != null) return stock.getCurrentPrice().doubleValue();
        return "USD".equals(stock.getCurrency()) ? 100.0 : 50_000.0;
    }

    private BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }
}
