package com.fintech.simulator.admin.controller;

import com.fintech.simulator.trading.domain.Order;
import com.fintech.simulator.trading.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 - 거래 모니터링 (PRD FR-10.10).
 * 전체 거래 페이지네이션. 이상 거래 탐지는 후속(룰 정의 필요).
 */
@RestController
@RequestMapping("/admin/trades")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTradesController {

    private final OrderRepository orderRepository;

    @GetMapping
    public Page<Order> list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "50") int size) {
        return orderRepository.findAll(PageRequest.of(Math.max(page, 0), Math.min(size, 100)));
    }
}
