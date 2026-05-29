package com.fintech.simulator.watchlist.repository;

import com.fintech.simulator.watchlist.domain.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    /** 사용자의 관심종목 — 최근 추가 순. */
    List<WatchlistItem> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByUserIdAndTicker(String userId, String ticker);

    /** 멱등 삭제 — 없으면 0건 삭제. 호출자(@Transactional) 안에서 실행. */
    long deleteByUserIdAndTicker(String userId, String ticker);
}
