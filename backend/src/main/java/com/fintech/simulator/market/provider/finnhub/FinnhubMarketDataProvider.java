package com.fintech.simulator.market.provider.finnhub;

import com.fintech.simulator.market.provider.MarketDataProvider;
import com.fintech.simulator.market.provider.Quote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Finnhub 시세 공급자.
 * - 실시간: WebSocket → PriceCache
 * - REST 폴백: FinnhubQuoteClient (/quote) — 무료 티어 현재가
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.external.finnhub.api-key")
public class FinnhubMarketDataProvider implements MarketDataProvider {

    private final FinnhubQuoteClient quoteClient;

    @Override
    public String name() { return "FINNHUB"; }

    @Override
    public boolean supports(String ticker) {
        // 영문 대문자 1~5자 (NASDAQ/NYSE 일반적 형식)
        return ticker != null && ticker.matches("[A-Z]{1,5}");
    }

    @Override
    public Optional<Quote> getQuote(String ticker) {
        return quoteClient.quote(ticker);
    }
}
