package com.fintech.simulator.market.provider;

import java.util.Optional;

/**
 * 시세 공급자 추상화 (OCP).
 * 구현체:
 *   - MockMarketDataProvider — 랜덤워크 (D07)
 *   - KisMarketDataProvider — 한국 KIS (D13)
 *   - FinnhubMarketDataProvider — 미국 Finnhub (D14)
 */
public interface MarketDataProvider {

    /** 식별자: "MOCK" / "KIS" / "FINNHUB" */
    String name();

    /** 해당 종목을 이 Provider가 다룰 수 있는지 (시장/통화 기준) */
    boolean supports(String ticker);

    /** 단건 현재가 조회. 미존재 시 Optional.empty(). */
    Optional<Quote> getQuote(String ticker);
}
