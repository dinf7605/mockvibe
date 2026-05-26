package com.fintech.simulator.trading.service;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.fx.FxRateProvider;
import com.fintech.simulator.market.cache.PriceCache;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.provider.MarketDataProvider;
import com.fintech.simulator.market.provider.Quote;
import com.fintech.simulator.market.repository.PriceHistoryRepository;
import com.fintech.simulator.market.repository.StockRepository;
import com.fintech.simulator.market.service.PriceLookupService;
import com.fintech.simulator.portfolio.domain.Holding;
import com.fintech.simulator.portfolio.domain.Wallet;
import com.fintech.simulator.portfolio.repository.HoldingRepository;
import com.fintech.simulator.portfolio.repository.WalletRepository;
import com.fintech.simulator.trading.TradingProperties;
import com.fintech.simulator.trading.domain.OrderSide;
import com.fintech.simulator.trading.dto.OrderResponse;
import com.fintech.simulator.trading.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradingServiceTest {

    @Mock StockRepository stockRepository;
    @Mock WalletRepository walletRepository;
    @Mock HoldingRepository holdingRepository;
    @Mock OrderRepository orderRepository;
    @Mock MarketDataProvider providerA;
    @Mock FxRateProvider fxRateProvider;
    @Mock PriceHistoryRepository priceHistoryRepository;

    PriceCache priceCache = new PriceCache();
    PriceLookupService priceLookup;
    TradingProperties props = new TradingProperties(new BigDecimal("0.00015"), new BigDecimal("0.0025"));
    TradingService service;

    @BeforeEach
    void setUp() {
        priceLookup = new PriceLookupService(priceCache, List.of(providerA), priceHistoryRepository);
        service = new TradingService(stockRepository, walletRepository, holdingRepository,
                orderRepository, priceLookup, fxRateProvider, props,
                event -> { /* no-op test publisher */ });
        priceCache.put(new Quote("005930", new BigDecimal("1000"), new BigDecimal("1000"), Instant.now()));
        given(fxRateProvider.rate(any(), any())).willReturn(BigDecimal.ONE);
        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    private Stock krxStock;

    /** given(...).willReturn(krxStock()) 같은 중첩 체인이 unfinished stubbing 에러를 일으키므로
     *  각 테스트 시작 시 별도 라인으로 미리 만들어둔다. */
    private void prepareStock() {
        krxStock = org.mockito.Mockito.mock(Stock.class);
        given(krxStock.getCurrency()).willReturn("KRW");
        given(krxStock.isActive()).willReturn(true);
    }

    @Test
    @DisplayName("buyMarket: 정상 매수 → 잔고 차감 + Holding 신규 생성 + ORDER 기록")
    void buy_success() {
        prepareStock();
        given(stockRepository.findById("005930")).willReturn(Optional.of(krxStock));
        Wallet wallet = Wallet.openWith("u1", new BigDecimal("100000"));
        given(walletRepository.findByUserIdForUpdate("u1")).willReturn(Optional.of(wallet));
        given(holdingRepository.findByUserIdAndTicker("u1", "005930")).willReturn(Optional.empty());

        OrderResponse r = service.buyMarket("u1", "005930", new BigDecimal("10"));

        assertThat(r.orderType()).isEqualTo(OrderSide.BUY);
        // 10000 * 1.00015 = 10001.5
        assertThat(r.totalAmountKrw()).isEqualByComparingTo("10001.50");
        assertThat(wallet.getCashBalance()).isEqualByComparingTo("89998.50");
    }

    @Test
    @DisplayName("buyMarket: 잔고 부족 → INSUFFICIENT_BALANCE, 어떤 상태도 변경 안 됨")
    void buy_insufficient_balance() {
        prepareStock();
        given(stockRepository.findById("005930")).willReturn(Optional.of(krxStock));
        Wallet wallet = Wallet.openWith("u1", new BigDecimal("500"));
        given(walletRepository.findByUserIdForUpdate("u1")).willReturn(Optional.of(wallet));

        assertThatThrownBy(() -> service.buyMarket("u1", "005930", BigDecimal.ONE))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("sellMarket: 정상 매도 → 잔고 입금 + Holding 차감")
    void sell_success() {
        prepareStock();
        given(stockRepository.findById("005930")).willReturn(Optional.of(krxStock));
        Wallet wallet = Wallet.openWith("u1", new BigDecimal("0"));
        given(walletRepository.findByUserIdForUpdate("u1")).willReturn(Optional.of(wallet));
        Holding h = Holding.newPosition("u1", "005930", new BigDecimal("10"), new BigDecimal("900"));
        given(holdingRepository.findByUserIdAndTicker("u1", "005930")).willReturn(Optional.of(h));

        OrderResponse r = service.sellMarket("u1", "005930", new BigDecimal("5"));

        assertThat(r.orderType()).isEqualTo(OrderSide.SELL);
        // 매도 5주 × 1000 = 5000, 수수료 0.75, 수령 4999.25
        assertThat(r.totalAmountKrw()).isEqualByComparingTo("4999.25");
        assertThat(wallet.getCashBalance()).isEqualByComparingTo("4999.25");
        assertThat(h.getQuantity()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("sellMarket: 보유 없으면 INSUFFICIENT_HOLDINGS")
    void sell_no_holding() {
        prepareStock();
        given(stockRepository.findById("005930")).willReturn(Optional.of(krxStock));
        given(walletRepository.findByUserIdForUpdate("u1"))
                .willReturn(Optional.of(Wallet.openWith("u1", BigDecimal.ZERO)));
        given(holdingRepository.findByUserIdAndTicker("u1", "005930")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.sellMarket("u1", "005930", BigDecimal.ONE))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_HOLDINGS);
    }

    @Test
    @DisplayName("sellMarket: 보유 수량 초과 매도 → INSUFFICIENT_HOLDINGS")
    void sell_over_quantity() {
        prepareStock();
        given(stockRepository.findById("005930")).willReturn(Optional.of(krxStock));
        given(walletRepository.findByUserIdForUpdate("u1"))
                .willReturn(Optional.of(Wallet.openWith("u1", BigDecimal.ZERO)));
        Holding h = Holding.newPosition("u1", "005930", new BigDecimal("3"), new BigDecimal("900"));
        given(holdingRepository.findByUserIdAndTicker("u1", "005930")).willReturn(Optional.of(h));

        assertThatThrownBy(() -> service.sellMarket("u1", "005930", new BigDecimal("10")))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_HOLDINGS);
    }
}
