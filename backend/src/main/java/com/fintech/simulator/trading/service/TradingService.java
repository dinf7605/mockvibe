package com.fintech.simulator.trading.service;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.fx.FxRateProvider;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.repository.StockRepository;
import com.fintech.simulator.market.service.PriceLookupService;
import com.fintech.simulator.market.service.PriceLookupService.PricePoint;
import com.fintech.simulator.portfolio.domain.Holding;
import com.fintech.simulator.portfolio.domain.Wallet;
import com.fintech.simulator.portfolio.repository.HoldingRepository;
import com.fintech.simulator.portfolio.repository.WalletRepository;
import com.fintech.simulator.trading.TradingProperties;
import com.fintech.simulator.trading.domain.Order;
import com.fintech.simulator.trading.domain.OrderSide;
import com.fintech.simulator.trading.dto.OrderResponse;
import com.fintech.simulator.trading.event.OrderExecutedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.fintech.simulator.trading.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 매매 서비스.
 *
 * 락 순서 (PRD §7.2 ADR-2, 데드락 방지):
 *     Wallet (PESSIMISTIC_WRITE) → Holdings (OPTIMISTIC @Version) → Orders (INSERT)
 *
 * 매수 흐름:
 *   1) Stock 활성·통화 확인
 *   2) 현재가 조회 (PriceCache → Provider 폴백)
 *   3) 수수료·환율 계산
 *   4) Wallet SELECT FOR UPDATE → 잔고 차감
 *   5) Holdings 평균단가 가중 평균 갱신 (또는 새 포지션)
 *   6) ORDERS INSERT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingService {

    private final StockRepository stockRepository;
    private final WalletRepository walletRepository;
    private final HoldingRepository holdingRepository;
    private final OrderRepository orderRepository;
    private final PriceLookupService priceLookup;
    private final FxRateProvider fxRateProvider;
    private final TradingProperties tradingProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse sellMarket(String userId, String ticker, BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        Stock stock = stockRepository.findById(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        PricePoint pricePoint = priceLookup.lookup(ticker);
        BigDecimal price = pricePoint.price();
        BigDecimal fxRate = fxRateProvider.rate(stock.getCurrency(), "KRW");
        BigDecimal grossKrw = price.multiply(quantity).multiply(fxRate);
        BigDecimal feeRate = tradingProperties.feeRateFor(stock.getCurrency());
        BigDecimal feeKrw = grossKrw.multiply(feeRate).setScale(4, RoundingMode.HALF_UP);
        BigDecimal proceedsKrw = grossKrw.subtract(feeKrw).setScale(2, RoundingMode.HALF_UP);

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Holding holding = holdingRepository.findByUserIdAndTicker(userId, ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_HOLDINGS));
        try {
            holding.reduceForSell(quantity);
            holdingRepository.save(holding);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Holdings optimistic lock conflict on SELL: user={}, ticker={}", userId, ticker);
            throw new BusinessException(ErrorCode.HOLDINGS_CONFLICT);
        }

        wallet.deposit(proceedsKrw);

        Order order = orderRepository.save(
                Order.marketSell(userId, ticker, price, quantity, fxRate, feeKrw, proceedsKrw)
        );

        log.info("Market SELL: user={}, ticker={}, qty={}, proceedsKRW={}, balanceAfter={}, priceSource={}",
                userId, ticker, quantity, proceedsKrw, wallet.getCashBalance(), pricePoint.source());
        eventPublisher.publishEvent(new OrderExecutedEvent(
                userId, ticker, OrderSide.SELL, price, quantity, proceedsKrw));
        return OrderResponse.from(order, wallet.getCashBalance(),
                pricePoint.source(), pricePoint.asOfDate());
    }

    @Transactional
    public OrderResponse buyMarket(String userId, String ticker, BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        Stock stock = stockRepository.findById(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
        if (!stock.isActive()) {
            throw new BusinessException(ErrorCode.STOCK_INACTIVE);
        }

        PricePoint pricePoint = priceLookup.lookup(ticker);
        BigDecimal price = pricePoint.price();
        BigDecimal fxRate = fxRateProvider.rate(stock.getCurrency(), "KRW");
        BigDecimal grossKrw = price.multiply(quantity).multiply(fxRate);
        BigDecimal feeRate = tradingProperties.feeRateFor(stock.getCurrency());
        BigDecimal feeKrw = grossKrw.multiply(feeRate).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalKrw = grossKrw.add(feeKrw).setScale(2, RoundingMode.HALF_UP);

        // 1) Wallet 비관적 락 — 잔고 차감 (음수면 withdraw가 BusinessException)
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        wallet.withdraw(totalKrw);

        // 2) Holdings 낙관적 락 — 평균단가 가중 평균 갱신
        BigDecimal buyPriceKrw = price.multiply(fxRate).setScale(2, RoundingMode.HALF_UP);
        try {
            Optional<Holding> existing = holdingRepository.findByUserIdAndTicker(userId, ticker);
            Holding holding = existing
                    .map(h -> { h.addBuy(quantity, buyPriceKrw); return h; })
                    .orElseGet(() -> Holding.newPosition(userId, ticker, quantity, buyPriceKrw));
            holdingRepository.save(holding);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 동시 매매 충돌 — 트랜잭션 롤백 후 사용자 재시도 유도
            log.warn("Holdings optimistic lock conflict: userId={}, ticker={}", userId, ticker);
            throw new BusinessException(ErrorCode.HOLDINGS_CONFLICT);
        }

        // 3) ORDERS 기록 (불변)
        Order order = orderRepository.save(
                Order.marketBuy(userId, ticker, price, quantity, fxRate, feeKrw, totalKrw)
        );

        log.info("Market BUY: user={}, ticker={}, qty={}, totalKRW={}, balanceAfter={}, priceSource={}",
                userId, ticker, quantity, totalKrw, wallet.getCashBalance(), pricePoint.source());
        eventPublisher.publishEvent(new OrderExecutedEvent(
                userId, ticker, OrderSide.BUY, price, quantity, totalKrw));
        return OrderResponse.from(order, wallet.getCashBalance(),
                pricePoint.source(), pricePoint.asOfDate());
    }

    /**
     * 지정가 체결 트랜잭션 (D22 LimitOrderProcessor → D23 Filler가 호출).
     * - 시장가와 동일한 락 순서: Wallet → Holdings → Orders
     * - 가격은 LIMIT_ORDERS.target_price (현재가 아님). 사용자 의도 보존.
     * @return 생성된 Order의 ID
     */
    @Transactional
    public Long fillLimit(String userId, String ticker, OrderSide side,
                          BigDecimal targetPrice, BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
        Stock stock = stockRepository.findById(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
        if (!stock.isActive()) throw new BusinessException(ErrorCode.STOCK_INACTIVE);

        BigDecimal fxRate = fxRateProvider.rate(stock.getCurrency(), "KRW");
        BigDecimal grossKrw = targetPrice.multiply(quantity).multiply(fxRate);
        BigDecimal feeRate = tradingProperties.feeRateFor(stock.getCurrency());
        BigDecimal feeKrw = grossKrw.multiply(feeRate).setScale(4, RoundingMode.HALF_UP);
        BigDecimal buyPriceKrw = targetPrice.multiply(fxRate).setScale(2, RoundingMode.HALF_UP);

        // Wallet 비관적 락
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (side == OrderSide.BUY) {
            BigDecimal totalKrw = grossKrw.add(feeKrw).setScale(2, RoundingMode.HALF_UP);
            wallet.withdraw(totalKrw);
            try {
                Optional<Holding> existing = holdingRepository.findByUserIdAndTicker(userId, ticker);
                Holding holding = existing
                        .map(h -> { h.addBuy(quantity, buyPriceKrw); return h; })
                        .orElseGet(() -> Holding.newPosition(userId, ticker, quantity, buyPriceKrw));
                holdingRepository.save(holding);
            } catch (ObjectOptimisticLockingFailureException e) {
                throw new BusinessException(ErrorCode.HOLDINGS_CONFLICT);
            }
            Order order = orderRepository.save(Order.limitBuy(
                    userId, ticker, targetPrice, quantity, fxRate, feeKrw, totalKrw));
            log.info("Limit BUY filled: user={} ticker={} qty={}@{} totalKRW={}",
                    userId, ticker, quantity, targetPrice, totalKrw);
            return order.getOrderId();
        } else {
            BigDecimal proceedsKrw = grossKrw.subtract(feeKrw).setScale(2, RoundingMode.HALF_UP);
            Holding holding = holdingRepository.findByUserIdAndTicker(userId, ticker)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_HOLDINGS));
            try {
                holding.reduceForSell(quantity);
                holdingRepository.save(holding);
            } catch (ObjectOptimisticLockingFailureException e) {
                throw new BusinessException(ErrorCode.HOLDINGS_CONFLICT);
            }
            wallet.deposit(proceedsKrw);
            Order order = orderRepository.save(Order.limitSell(
                    userId, ticker, targetPrice, quantity, fxRate, feeKrw, proceedsKrw));
            log.info("Limit SELL filled: user={} ticker={} qty={}@{} proceedsKRW={}",
                    userId, ticker, quantity, targetPrice, proceedsKrw);
            return order.getOrderId();
        }
    }

}
