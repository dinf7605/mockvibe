package com.fintech.simulator.trading.controller;

import com.fintech.simulator.common.idempotency.IdempotencyService;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final IdempotencyService idempotencyService;

    /**
     * 시장가 매수. 클라이언트가 {@code Idempotency-Key} 헤더를 보내면 중복 제출(재시도·
     * 더블클릭)이 한 번만 체결되고, 재요청에는 첫 응답을 그대로 돌려준다.
     */
    @PostMapping("/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse buyMarket(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BuyMarketRequest request
    ) {
        return idempotencyService.execute(userId, idempotencyKey, OrderResponse.class,
                () -> tradingService.buyMarket(userId, request.ticker(), request.quantity()));
    }

    @PostMapping("/sell")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse sellMarket(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody SellMarketRequest request
    ) {
        return idempotencyService.execute(userId, idempotencyKey, OrderResponse.class,
                () -> tradingService.sellMarket(userId, request.ticker(), request.quantity()));
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
