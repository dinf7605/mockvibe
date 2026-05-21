package com.fintech.simulator.risk.controller;

import com.fintech.simulator.risk.dto.RiskResponse;
import com.fintech.simulator.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping
    public RiskResponse get(@AuthenticationPrincipal String userId) {
        return riskService.compute(userId);
    }
}
