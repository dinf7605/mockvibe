package com.fintech.simulator.market.repository;

import com.fintech.simulator.market.domain.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByTickerAndTradeDateBetweenOrderByTradeDateAsc(
            String ticker, LocalDate from, LocalDate to);

    long countByTicker(String ticker);

    /**
     * 가장 최근(=가장 큰 trade_date) 일봉 1건.
     * 장 외 시간 매매 시 currentPrice fallback 으로 사용.
     */
    Optional<PriceHistory> findTopByTickerOrderByTradeDateDesc(String ticker);

    /**
     * 종목별 최근 N일 일봉 (오래된 → 최신 순으로 차트에 그대로 사용 가능).
     */
    List<PriceHistory> findTop365ByTickerOrderByTradeDateDesc(String ticker);

    /**
     * UPSERT 용도: 동일 (ticker, trade_date) row 존재 여부 검사.
     * V4 UNIQUE 제약 (uk_price_history_t_d) 충돌 회피.
     */
    Optional<PriceHistory> findByTickerAndTradeDate(String ticker, LocalDate tradeDate);
}
