package com.fintech.simulator.market.service;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.cache.PriceCache;
import com.fintech.simulator.market.domain.PriceHistory;
import com.fintech.simulator.market.provider.MarketDataProvider;
import com.fintech.simulator.market.provider.Quote;
import com.fintech.simulator.market.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 매매·평가 시 사용할 가격을 조회한다.
 *
 * 우선순위:
 *   1) PriceCache 의 최근 tick   (장 시간 중, STOMP 로 들어온 실시간)
 *   2) MarketDataProvider 의 직접 조회 (캐시 miss 시 폴백)
 *   3) PRICE_HISTORY 의 최근 종가 (장 외 시간 + Provider 실패 시 모의투자 가능하게)
 *
 * 셋 다 실패하면 PRICE_NOT_AVAILABLE 예외.
 *
 * @see ADR-010 (운영 가격 조회 폴백)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceLookupService {

    private final PriceCache priceCache;
    private final List<MarketDataProvider> providers;
    private final PriceHistoryRepository priceHistoryRepository;

    /**
     * 매매 호출자가 가격 + 출처를 함께 받을 때 사용.
     * 응답 DTO 에 source 를 노출하면 프론트가 "종가 기준" 배지를 표시할 수 있다.
     */
    public PricePoint lookup(String ticker) {
        // 1) PriceCache (장 시간 중 STOMP 로 흘러들어온 실시간 가격)
        Optional<Quote> live = priceCache.get(ticker);
        if (live.isPresent()) {
            return new PricePoint(live.get().price(), PriceSource.LIVE, null);
        }

        // 2) Provider 직접 조회 (캐시 비어있지만 외부 살아있는 경우)
        Optional<Quote> fromProvider = providers.stream()
                .filter(p -> p.supports(ticker))
                .map(p -> p.getQuote(ticker))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        if (fromProvider.isPresent()) {
            return new PricePoint(fromProvider.get().price(), PriceSource.LIVE, null);
        }

        // 3) PRICE_HISTORY 의 최근 종가
        Optional<PriceHistory> latest = priceHistoryRepository.findTopByTickerOrderByTradeDateDesc(ticker);
        if (latest.isPresent()) {
            PriceHistory h = latest.get();
            log.debug("Price for {} from PRICE_HISTORY close on {} = {}",
                    ticker, h.getTradeDate(), h.getClose());
            return new PricePoint(h.getClose(), PriceSource.CLOSE, h.getTradeDate());
        }

        throw new BusinessException(ErrorCode.PRICE_NOT_AVAILABLE);
    }

    /** 가격만 빠르게 필요한 호출용 (출처 불필요). */
    public BigDecimal currentPrice(String ticker) {
        return lookup(ticker).price();
    }

    public enum PriceSource {
        /** STOMP 로 흘러들어온 실시간 시세 (정상 장 시간) */
        LIVE,
        /** PRICE_HISTORY 의 가장 최근 종가 (장 외 시간 폴백) */
        CLOSE
    }

    /**
     * 가격 조회 결과. 매매 응답 DTO 에 source 를 포함해 UX 안내에 사용.
     *
     * @param price       체결가
     * @param source      가격 출처 (LIVE / CLOSE)
     * @param asOfDate    source=CLOSE 일 때 그 종가의 거래일. LIVE 일 때 null.
     */
    public record PricePoint(BigDecimal price, PriceSource source, LocalDate asOfDate) {}
}
