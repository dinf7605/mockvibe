package com.fintech.simulator.portfolio.service;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.fx.FxRateProvider;
import com.fintech.simulator.market.cache.PriceCache;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.repository.StockRepository;
import com.fintech.simulator.portfolio.domain.Holding;
import com.fintech.simulator.portfolio.dto.PortfolioResponse;
import com.fintech.simulator.portfolio.dto.PortfolioResponse.HoldingItem;
import com.fintech.simulator.portfolio.dto.PortfolioResponse.RegionShare;
import com.fintech.simulator.portfolio.repository.HoldingRepository;
import com.fintech.simulator.portfolio.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포트폴리오 스냅샷.
 *   - 평가손익 = 현재가(KRW 환산) - 평균단가(KRW)
 *   - 미국 종목은 fxRate(USD→KRW) 적용
 *   - 현재가는 PriceCache 우선, 없으면 Stock.current_price (Mock provider가 갱신)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final WalletRepository walletRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;
    private final PriceCache priceCache;
    private final FxRateProvider fxRateProvider;

    public PortfolioResponse get(String userId) {
        BigDecimal cash = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
                .getCashBalance();

        List<Holding> holdings = holdingRepository.findAllByUserId(userId).stream()
                .filter(h -> h.getQuantity().signum() > 0)
                .toList();

        // Stock 마스터 일괄 조회 (N+1 회피)
        List<String> tickers = holdings.stream().map(Holding::getTicker).toList();
        Map<String, Stock> stockMap = stockRepository.findAllById(tickers).stream()
                .collect(Collectors.toMap(Stock::getTicker, s -> s));

        List<HoldingItem> items = new ArrayList<>();
        BigDecimal holdingValue = BigDecimal.ZERO;
        BigDecimal costTotal = BigDecimal.ZERO;
        BigDecimal krValue = BigDecimal.ZERO;
        BigDecimal usValue = BigDecimal.ZERO;

        for (Holding h : holdings) {
            Stock stock = stockMap.get(h.getTicker());
            if (stock == null) continue;

            BigDecimal currentNative = priceCache.get(h.getTicker())
                    .map(q -> q.price())
                    .orElseGet(() -> stock.getCurrentPrice() != null ? stock.getCurrentPrice() : h.getAveragePriceKrw());

            BigDecimal fxRate = fxRateProvider.rate(stock.getCurrency(), "KRW");
            BigDecimal currentKrw = currentNative.multiply(fxRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal evaluation = currentKrw.multiply(h.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cost = h.getAveragePriceKrw().multiply(h.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pnl = evaluation.subtract(cost);
            BigDecimal pnlPct = cost.signum() == 0 ? BigDecimal.ZERO
                    : pnl.multiply(BigDecimal.valueOf(100)).divide(cost, 2, RoundingMode.HALF_UP);

            items.add(new HoldingItem(
                    stock.getTicker(), stock.getCompanyName(), stock.getMarket(), stock.getCurrency(),
                    h.getQuantity(), h.getAveragePriceKrw(), currentKrw, evaluation, pnl, pnlPct
            ));

            holdingValue = holdingValue.add(evaluation);
            costTotal = costTotal.add(cost);
            if ("KR".equals(stock.getRegion())) krValue = krValue.add(evaluation);
            else if ("US".equals(stock.getRegion())) usValue = usValue.add(evaluation);
        }

        BigDecimal total = cash.add(holdingValue);
        BigDecimal totalPnl = holdingValue.subtract(costTotal);
        BigDecimal totalPnlPct = costTotal.signum() == 0 ? BigDecimal.ZERO
                : totalPnl.multiply(BigDecimal.valueOf(100)).divide(costTotal, 2, RoundingMode.HALF_UP);

        RegionShare share = computeShare(total, krValue, usValue, cash);

        return new PortfolioResponse(cash, holdingValue, total, costTotal, totalPnl, totalPnlPct, items, share);
    }

    private RegionShare computeShare(BigDecimal total, BigDecimal kr, BigDecimal us, BigDecimal cash) {
        if (total.signum() == 0) return new RegionShare(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100));
        return new RegionShare(
                kr.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP),
                us.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP),
                cash.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP)
        );
    }
}
