package com.fintech.simulator.alert.service;

import com.fintech.simulator.alert.domain.AlertStatus;
import com.fintech.simulator.alert.domain.PriceAlert;
import com.fintech.simulator.alert.repository.PriceAlertRepository;
import com.fintech.simulator.market.event.PriceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 시세 갱신 이벤트 기반 가격 알림 트리거 처리기.
 *
 * <p>{@link com.fintech.simulator.trading.scheduler.LimitOrderProcessor} 와 동일한 패턴:
 * {@link PriceUpdatedEvent} 를 비동기로 받아, 해당 종목의 ACTIVE 알림을 평가해 도달 시 TRIGGERED 로 전이.
 *
 * <h3>트랜잭션</h3>
 * 이 메서드는 이벤트 퍼블리셔가 프록시로 호출하는 진입점이라 self-invocation 이 아니지만,
 * 안전하게 양쪽 모두 명시적 {@code save()} 한다 (findByTickerAndStatus 로 가져온 엔티티는
 * 그 조회 트랜잭션이 끝나면 detached → save 로 merge 해야 변경이 반영됨).
 *
 * <h3>동시성</h3>
 * 동일 종목 시세가 빠르게 연속 갱신될 때 같은 알림을 중복 트리거하지 않도록 per-ticker 락.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceAlertProcessor {

    private final PriceAlertRepository alertRepository;
    private final ConcurrentMap<String, Object> tickerLocks = new ConcurrentHashMap<>();

    @Async
    @EventListener
    public void on(PriceUpdatedEvent event) {
        String ticker = event.quote().ticker();
        BigDecimal price = event.quote().price();
        if (price == null) return;

        Object lock = tickerLocks.computeIfAbsent(ticker, k -> new Object());
        synchronized (lock) {
            List<PriceAlert> actives = alertRepository.findByTickerAndStatus(ticker, AlertStatus.ACTIVE);
            for (PriceAlert alert : actives) {
                if (alert.matches(price)) {
                    alert.trigger(price);
                    alertRepository.save(alert);
                    log.debug("PriceAlert triggered: id={} ticker={} {} {} @ {}",
                            alert.getAlertId(), ticker, alert.getDirection(), alert.getTargetPrice(), price);
                }
            }
        }
    }
}
