package com.fintech.simulator.watchlist.repository;

import com.fintech.simulator.watchlist.domain.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    /** 사용자의 관심종목 — 최근 추가 순. */
    List<WatchlistItem> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByUserIdAndTicker(String userId, String ticker);

    /**
     * 멱등 삭제 — 없으면 0건. 호출자(@Transactional) 안에서 실행.
     *
     * <p>파생 {@code deleteBy...} 는 (엔티티 로드 후 삭제 → 반환 캐스팅) 경로에서
     * ClassCastException 을 유발할 수 있어, 명시적 벌크 DELETE 로 처리한다.
     */
    @Modifying
    @Query("delete from WatchlistItem w where w.userId = :userId and w.ticker = :ticker")
    int deleteByUserIdAndTicker(@Param("userId") String userId, @Param("ticker") String ticker);
}
