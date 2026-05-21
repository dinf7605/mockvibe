package com.fintech.simulator.admin.controller;

import com.fintech.simulator.market.cache.PriceCache;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 관리자 - 시스템 운영 (PRD FR-10.13~10.16).
 * Circuit Breaker 상태/Reset, 캐시 메트릭 등.
 */
@RestController
@RequestMapping("/admin/system")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSystemController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final PriceCache priceCache;

    @GetMapping("/circuit-breakers")
    public List<CbInfo> circuitBreakers() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .map(cb -> new CbInfo(
                        cb.getName(),
                        cb.getState().name(),
                        cb.getMetrics().getFailureRate(),
                        cb.getMetrics().getNumberOfFailedCalls(),
                        cb.getMetrics().getNumberOfSuccessfulCalls()
                ))
                .toList();
    }

    @PostMapping("/circuit-breakers/{name}/reset")
    public Map<String, String> reset(@PathVariable String name) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        cb.reset();
        return Map.of("name", name, "state", cb.getState().name());
    }

    @GetMapping("/cache")
    public Map<String, Object> cache() {
        return Map.of("priceCacheSize", priceCache.size());
    }

    public record CbInfo(String name, String state, float failureRate,
                         long failedCalls, long successCalls) {}
}
