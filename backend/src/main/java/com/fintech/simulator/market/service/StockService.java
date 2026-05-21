package com.fintech.simulator.market.service;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.controller.StockResponse;
import com.fintech.simulator.market.controller.StockSearchResponse;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private static final int MAX_PAGE_SIZE = 100;

    private final StockRepository stockRepository;

    public StockResponse get(String ticker) {
        Stock s = stockRepository.findById(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
        return StockResponse.from(s);
    }

    public StockSearchResponse search(String q, String market, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.ASC, "ticker"));
        Page<Stock> result = stockRepository.search(q, market, pageable);
        return StockSearchResponse.from(result.map(StockResponse::from));
    }
}
