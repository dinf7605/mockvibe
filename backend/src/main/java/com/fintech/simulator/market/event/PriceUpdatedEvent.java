package com.fintech.simulator.market.event;

import com.fintech.simulator.market.provider.Quote;

/**
 * Provider가 새 시세를 받았을 때 발행.
 * - PriceBroadcaster: STOMP `/topic/price/{ticker}` 브로드캐스트
 * - LimitOrderProcessor(D22): 지정가 체결 후보 조회
 */
public record PriceUpdatedEvent(Quote quote) {}
