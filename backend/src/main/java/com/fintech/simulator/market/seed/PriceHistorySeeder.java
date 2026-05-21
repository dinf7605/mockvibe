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
import java.util.List;
import java.util.Random;

/**
 * 1년치 OHLC 시드 (Mock 랜덤워크). 외부 API 키 없이도 D27~D31이 동작하도록 보장.
 * KIS/Finnhub 실연동은 후속 Phase에서 동일 테이블에 INSERT/UPSERT.
 *
 * - 종목별로 이미 데이터가 있으면 skip (idempotent)
 * - 주말(토/일) skip
 * - 일별 변동: ±2% 정규근사 → 기본가 → OHLC 약간 흔들기
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceHistorySeeder implements ApplicationRunner {

    private static final int DAYS = 365;

    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Stock> stocks = stockRepository.findAll();
        int seeded = 0;
        for (Stock s : stocks) {
            if (priceHistoryRepository.countByTicker(s.getTicker()) > 0) continue;
            generate(s);
            seeded++;
        }
        if (seeded > 0) log.info("PRICE_HISTORY seeded: {} tickers × {} days", seeded, DAYS);
    }

    private void generate(Stock stock) {
        Random rng = new Random(stock.getTicker().hashCode());
        double basePrice = stock.getCurrentPrice() != null
                ? stock.getCurrentPrice().doubleValue()
                : ("USD".equals(stock.getCurrency()) ? 100.0 : 50_000.0);

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
    }

    private BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }
}
