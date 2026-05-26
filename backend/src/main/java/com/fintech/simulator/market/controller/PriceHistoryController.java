package com.fintech.simulator.market.controller;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.domain.PriceHistory;
import com.fintech.simulator.market.dto.DailyCandle;
import com.fintech.simulator.market.repository.PriceHistoryRepository;
import com.fintech.simulator.market.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * 일봉 (PRICE_HISTORY) 조회 endpoint.
 *
 * - 차트 표시 (StockDetailPage 의 lightweight-charts)
 * - 장 외 시간 매매 안내 ("종가 기준 ₩X")
 */
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class PriceHistoryController {

    private static final int DEFAULT_DAYS = 90;
    private static final int MAX_DAYS = 365;

    private final PriceHistoryRepository priceHistoryRepository;
    private final StockRepository stockRepository;

    /**
     * 최근 N일 일봉.
     * GET /stocks/{ticker}/history?days=90
     *
     * 응답: 오래된 날짜 → 최신 날짜 순. lightweight-charts 가 그대로 받아 그릴 수 있다.
     */
    @GetMapping("/{ticker}/history")
    public List<DailyCandle> history(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "90") int days
    ) {
        if (stockRepository.findById(ticker).isEmpty()) {
            throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
        }
        int clampedDays = Math.max(1, Math.min(days, MAX_DAYS));

        // DB 인덱스에 맞춘 desc 조회 후, 클라이언트 차트가 기대하는 ASC 로 재정렬
        List<PriceHistory> rows = priceHistoryRepository
                .findTop365ByTickerOrderByTradeDateDesc(ticker);

        return rows.stream()
                .limit(clampedDays)
                .sorted(Comparator.comparing(PriceHistory::getTradeDate))
                .map(DailyCandle::from)
                .toList();
    }

    /**
     * 가장 최근 종가 1건 — 매매 패널의 "종가 기준" 표시용.
     * GET /stocks/{ticker}/last-close
     */
    @GetMapping("/{ticker}/last-close")
    public LastCloseResponse lastClose(@PathVariable String ticker) {
        if (stockRepository.findById(ticker).isEmpty()) {
            throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
        }
        return priceHistoryRepository.findTopByTickerOrderByTradeDateDesc(ticker)
                .map(LastCloseResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICE_NOT_AVAILABLE));
    }

    public record LastCloseResponse(
            String ticker,
            LocalDate tradeDate,
            java.math.BigDecimal close
    ) {
        static LastCloseResponse from(PriceHistory h) {
            return new LastCloseResponse(h.getTicker(), h.getTradeDate(), h.getClose());
        }
    }
}
