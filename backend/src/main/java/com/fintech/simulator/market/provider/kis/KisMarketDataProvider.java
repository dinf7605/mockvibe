package com.fintech.simulator.market.provider.kis;

import com.fintech.simulator.market.provider.MarketDataProvider;
import com.fintech.simulator.market.provider.Quote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * KIS 시세 공급자 (D12: REST 인증·호출 골격 / D13: WebSocket 구독으로 본격 시세 수신).
 *
 * - supports: KRX(한국) 종목 (6자리 숫자 ticker)
 * - getQuote: D13까지는 빈 Optional (PriceCache로부터 받아간 값을 다른 Provider가 못 채울 때 폴백)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisMarketDataProvider implements MarketDataProvider {

    private static final Set<String> SUPPORTED_MARKETS = Set.of("KRX");

    private final KisRestClient restClient;

    @Override
    public String name() {
        return "KIS";
    }

    @Override
    public boolean supports(String ticker) {
        // KRX 종목은 보통 6자리 숫자
        return ticker != null && ticker.matches("\\d{6}");
    }

    @Override
    public Optional<Quote> getQuote(String ticker) {
        // D13에서 KIS REST `/uapi/domestic-stock/v1/quotations/inquire-price` 호출로 구현
        log.debug("KIS REST quote stub for {} (markets={})", ticker, SUPPORTED_MARKETS);
        return Optional.empty();
    }
}
