package com.fintech.simulator.trading.controller;

import com.fintech.simulator.trading.dto.LimitOrderRequest;
import com.fintech.simulator.trading.dto.LimitOrderResponse;
import com.fintech.simulator.trading.service.LimitOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders/limit")
@RequiredArgsConstructor
public class LimitOrderController {

    private final LimitOrderService limitOrderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LimitOrderResponse register(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody LimitOrderRequest request
    ) {
        return limitOrderService.register(userId, request);
    }

    @GetMapping
    public LimitOrderResponse.Page_ list(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return limitOrderService.list(userId, page, size);
    }

    @DeleteMapping("/{limitOrderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @AuthenticationPrincipal String userId,
            @PathVariable Long limitOrderId
    ) {
        limitOrderService.cancel(userId, limitOrderId);
    }
}
