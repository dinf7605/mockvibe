package com.fintech.simulator.market.provider.finnhub;

import com.fintech.simulator.market.provider.MarketDataProvider;
import com.fintech.simulator.market.provider.Quote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Finnhub 시세 공급자.
 * 실시간 가격은 WebSocket으로 들어와 PriceCache에 저장됨 → MarketController에서 캐시 우선 조회.
 * REST 폴백은 D26 PRICE_HISTORY 적재 시 활용.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.external.finnhub.api-key")
public class FinnhubMarketDataProvider implements MarketDataProvider {

    @Override
    public String name() { return "FINNHUB"; }

    @Override
    public boolean supports(String ticker) {
        // 영문 대문자 1~5자 (NASDAQ/NYSE 일반적 형식)
        return ticker != null && ticker.matches("[A-Z]{1,5}");
    }

    @Override
    public Optional<Quote> getQuote(String ticker) {
        // 실시간은 WebSocket → PriceCache가 채움. REST 단건 조회는 D26에서 추가.
        log.debug("Finnhub REST quote stub for {}", ticker);
        return Optional.empty();
    }
}
