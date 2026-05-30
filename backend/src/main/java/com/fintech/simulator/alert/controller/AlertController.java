package com.fintech.simulator.alert.controller;

import com.fintech.simulator.alert.service.PriceAlertService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 가격 알림 API (로그인 필요).
 *
 * <ul>
 *   <li>POST   /alerts            — 알림 생성</li>
 *   <li>GET    /alerts            — 내 알림 목록</li>
 *   <li>GET    /alerts/triggered-count — 미확인(TRIGGERED) 개수 (벨 배지용)</li>
 *   <li>DELETE /alerts/{alertId}  — 취소</li>
 * </ul>
 */
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final PriceAlertService alertService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertResponse create(@AuthenticationPrincipal String userId,
                                @Valid @RequestBody CreateAlertRequest request) {
        return AlertResponse.from(
                alertService.create(userId, request.ticker(), request.direction(), request.targetPrice()));
    }

    @GetMapping
    public List<AlertResponse> list(@AuthenticationPrincipal String userId) {
        return alertService.list(userId).stream().map(AlertResponse::from).toList();
    }

    @GetMapping("/triggered-count")
    public TriggeredCountResponse triggeredCount(@AuthenticationPrincipal String userId) {
        return new TriggeredCountResponse(alertService.triggeredCount(userId));
    }

    @DeleteMapping("/{alertId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal String userId, @PathVariable Long alertId) {
        alertService.cancel(userId, alertId);
    }

    public record TriggeredCountResponse(long count) {}
}
