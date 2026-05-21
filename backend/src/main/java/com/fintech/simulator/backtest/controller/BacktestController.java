package com.fintech.simulator.backtest.controller;

import com.fintech.simulator.backtest.dto.BacktestRequest;
import com.fintech.simulator.backtest.dto.BacktestResponse;
import com.fintech.simulator.backtest.service.BacktestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestService backtestService;

    @PostMapping("/run")
    public BacktestResponse run(@AuthenticationPrincipal String userId,
                                @Valid @RequestBody BacktestRequest request) {
        return backtestService.run(userId, request);
    }
}
