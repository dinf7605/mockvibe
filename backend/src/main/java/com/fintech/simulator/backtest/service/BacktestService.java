package com.fintech.simulator.backtest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.simulator.backtest.domain.BacktestRun;
import com.fintech.simulator.backtest.dto.BacktestRequest;
import com.fintech.simulator.backtest.dto.BacktestResponse;
import com.fintech.simulator.backtest.engine.BacktestEngine;
import com.fintech.simulator.backtest.engine.BacktestEngine.Result;
import com.fintech.simulator.backtest.repository.BacktestRunRepository;
import com.fintech.simulator.backtest.strategy.Strategy;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.domain.PriceHistory;
import com.fintech.simulator.market.repository.PriceHistoryRepository;
import com.fintech.simulator.market.repository.StockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BacktestService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final StockRepository stockRepository;
    private final BacktestRunRepository backtestRunRepository;
    private final BacktestEngine engine;
    private final ObjectMapper objectMapper;
    private final Map<String, Strategy> strategiesByName;

    public BacktestService(PriceHistoryRepository priceHistoryRepository,
                           StockRepository stockRepository,
                           BacktestRunRepository backtestRunRepository,
                           BacktestEngine engine,
                           ObjectMapper objectMapper,
                           List<Strategy> strategies) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.stockRepository = stockRepository;
        this.backtestRunRepository = backtestRunRepository;
        this.engine = engine;
        this.objectMapper = objectMapper;
        this.strategiesByName = strategies.stream()
                .collect(java.util.stream.Collectors.toMap(Strategy::name, s -> s));
    }

    @Transactional
    public BacktestResponse run(String userId, BacktestRequest req) {
        Strategy strategy = strategiesByName.get(req.strategy());
        if (strategy == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "지원하지 않는 전략: " + req.strategy());
        }
        if (stockRepository.findById(req.ticker()).isEmpty()) {
            throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
        }
        if (!req.startDate().isBefore(req.endDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "startDate는 endDate 이전이어야 합니다.");
        }

        List<PriceHistory> history = priceHistoryRepository
                .findByTickerAndTradeDateBetweenOrderByTradeDateAsc(
                        req.ticker(), req.startDate(), req.endDate());
        if (history.size() < 2) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "해당 기간 시계열이 부족합니다 (PRICE_HISTORY 미적재 가능).");
        }

        Result result = engine.run(strategy, history, req.initialCapital());

        BacktestRun run = BacktestRun.of(
                userId, strategy.name(), null, req.ticker(),
                req.startDate(), req.endDate(), req.initialCapital(),
                result.finalValue(), result.totalReturn(), result.mdd(), result.sharpe(),
                result.tradeCount(), result.winRate(),
                serializeDetail(result));
        backtestRunRepository.save(run);

        log.info("Backtest: user={} {} {} {}→{} return={} mdd={} sharpe={} trades={}",
                userId, strategy.name(), req.ticker(), req.startDate(), req.endDate(),
                result.totalReturn(), result.mdd(), result.sharpe(), result.tradeCount());

        return BacktestResponse.of(run.getRunId(), req, result);
    }

    private String serializeDetail(Result r) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "equityCurve", r.equityCurve(),
                    "trades", r.trades()));
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
