package com.fintech.simulator.market.controller;

import org.springframework.data.domain.Page;

import java.util.List;

public record StockSearchResponse(
        List<StockResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static StockSearchResponse from(Page<StockResponse> p) {
        return new StockSearchResponse(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages()
        );
    }
}
