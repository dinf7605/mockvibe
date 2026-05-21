package com.fintech.simulator.trading.controller;

import com.fintech.simulator.trading.dto.BuyMarketRequest;
import com.fintech.simulator.trading.dto.OrderResponse;
import com.fintech.simulator.trading.dto.SellMarketRequest;
import com.fintech.simulator.trading.dto.TradeHistoryResponse;
import com.fintech.simulator.trading.repository.OrderRepository;
import com.fintech.simulator.trading.service.TradingService;
import org.springframework.data.domain.PageRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradingController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TradingService tradingService;
    private final OrderRepository orderRepository;

    @PostMapping("/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse buyMarket(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody BuyMarketRequest request
    ) {
        return tradingService.buyMarket(userId, request.ticker(), request.quantity());
    }

    @PostMapping("/sell")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse sellMarket(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SellMarketRequest request
    ) {
        return tradingService.sellMarket(userId, request.ticker(), request.quantity());
    }

    @GetMapping("/history")
    public TradeHistoryResponse history(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safe = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return TradeHistoryResponse.from(
                orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), safe)));
    }
}
