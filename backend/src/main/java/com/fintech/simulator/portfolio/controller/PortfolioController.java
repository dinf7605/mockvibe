package com.fintech.simulator.portfolio.controller;

import com.fintech.simulator.portfolio.dto.PortfolioResponse;
import com.fintech.simulator.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public PortfolioResponse get(@AuthenticationPrincipal String userId) {
        return portfolioService.get(userId);
    }
}
