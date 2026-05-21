package com.fintech.simulator.market.provider.finnhub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 부팅 시 미국 상위 종목 자동 구독.
 *
 * PRD §10 #3 "동적 구독(보는 종목만)" 원칙이지만, 데모/검증용으로 일부 고정 종목 선구독.
 * 후속에 사용자 종목 상세 페이지 진입 시점에 동적 subscribe로 교체 권장.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.external.finnhub.api-key")
public class FinnhubAutoSubscriber implements ApplicationRunner {

    private static final List<String> SEED_TICKERS = List.of("AAPL", "MSFT", "NVDA", "GOOGL", "TSLA");

    private final FinnhubWebSocketClient client;

    @Override
    public void run(ApplicationArguments args) {
        for (String t : SEED_TICKERS) {
            try {
                client.subscribe(t);
            } catch (Exception e) {
                log.warn("Finnhub initial subscribe failed: {} {}", t, e.getMessage());
            }
        }
        log.info("Finnhub seed subscriptions: {}", SEED_TICKERS);
    }
}
