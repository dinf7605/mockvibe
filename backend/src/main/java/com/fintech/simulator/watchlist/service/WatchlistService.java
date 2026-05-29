package com.fintech.simulator.watchlist.service;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.controller.StockResponse;
import com.fintech.simulator.market.repository.StockRepository;
import com.fintech.simulator.watchlist.domain.WatchlistItem;
import com.fintech.simulator.watchlist.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관심종목 즐겨찾기.
 *
 * <p>add/remove 는 모두 멱등(idempotent): 이미 있는 종목을 또 추가하거나 없는 종목을
 * 삭제해도 예외 없이 통과한다 (별 토글 UX 와 잘 맞고 중복 클릭에 안전).
 */
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final StockRepository stockRepository;

    /** 관심종목 목록 — STOCKS 와 합쳐 종목명·현재가까지 포함한 표시용 DTO 로 반환. */
    @Transactional(readOnly = true)
    public List<StockResponse> list(String userId) {
        return watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WatchlistItem::getTicker)
                .map(stockRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(StockResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean contains(String userId, String ticker) {
        return watchlistRepository.existsByUserIdAndTicker(userId, ticker);
    }

    @Transactional
    public void add(String userId, String ticker) {
        if (stockRepository.findById(ticker).isEmpty()) {
            throw new BusinessException(ErrorCode.STOCK_NOT_FOUND);
        }
        if (watchlistRepository.existsByUserIdAndTicker(userId, ticker)) {
            return;   // 멱등: 이미 등록됨
        }
        watchlistRepository.save(WatchlistItem.of(userId, ticker));
    }

    @Transactional
    public void remove(String userId, String ticker) {
        watchlistRepository.deleteByUserIdAndTicker(userId, ticker);   // 멱등
    }
}
