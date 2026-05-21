package com.fintech.simulator.market.controller;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.cache.PriceCache;
import com.fintech.simulator.market.provider.MarketDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 시세 조회 API.
 * 우선순위: 캐시 → 각 Provider 직접 조회 → 404.
 * 실시간 시세는 D11+ STOMP `/topic/price/{ticker}`로 push.
 */
@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final PriceCache priceCache;
    private final List<MarketDataProvider> providers;

    @GetMapping("/price/{ticker}")
    public PriceResponse getPrice(@PathVariable String ticker) {
        return priceCache.get(ticker)
                .or(() -> providers.stream()
                        .filter(p -> p.supports(ticker))
                        .map(p -> p.getQuote(ticker))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst())
                .map(PriceResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICE_NOT_AVAILABLE));
    }
}
