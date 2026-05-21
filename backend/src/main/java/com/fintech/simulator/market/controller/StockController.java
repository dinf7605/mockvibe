package com.fintech.simulator.market.controller;

import com.fintech.simulator.market.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 종목 마스터 조회·검색 API.
 * - GET /stocks/{ticker}
 * - GET /stocks/search?q=&market=&page=&size=
 */
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/{ticker}")
    public StockResponse get(@PathVariable String ticker) {
        return stockService.get(ticker);
    }

    @GetMapping("/search")
    public StockSearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return stockService.search(q, market, page, size);
    }
}
