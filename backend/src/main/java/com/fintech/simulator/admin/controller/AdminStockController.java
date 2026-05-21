package com.fintech.simulator.admin.controller;

import com.fintech.simulator.admin.audit.Auditable;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 - 종목 관리 (PRD FR-10.7~10.9).
 * D03 ERD 기준 Stock 도메인은 isActive를 setter 없이 노출. 토글 도메인 메서드 추가 필요 시 후속.
 * 현 단계: 활성 토글만 도메인 메서드로 노출.
 */
@RestController
@RequestMapping("/admin/stocks")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminStockController {

    private final StockRepository stockRepository;

    @GetMapping
    public Page<Stock> list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "30") int size) {
        return stockRepository.findAll(PageRequest.of(page, Math.min(size, 100)));
    }

    @PostMapping("/{ticker}/toggle")
    @Auditable(action = "TOGGLE_STOCK", targetType = "STOCK", targetIdParam = "ticker")
    @Transactional
    public Stock toggleActive(@PathVariable String ticker) {
        Stock s = stockRepository.findById(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
        s.toggleActive();
        return s;
    }
}
