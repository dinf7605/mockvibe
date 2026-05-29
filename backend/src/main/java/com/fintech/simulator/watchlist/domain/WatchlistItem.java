package com.fintech.simulator.watchlist.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 관심종목 한 건 (user_id × ticker).
 *
 * <p>UNIQUE(user_id, ticker) 로 중복 등록을 막는다. 가격·종목명 등 표시용 데이터는
 * STOCKS 에서 조회해 합치므로 이 엔티티는 즐겨찾기 관계만 보관한다.
 */
@Entity
@Table(name = "WATCHLIST")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "watchlist_id")
    private Long watchlistId;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    private WatchlistItem(String userId, String ticker, OffsetDateTime createdAt) {
        this.userId = userId;
        this.ticker = ticker;
        this.createdAt = createdAt;
    }

    public static WatchlistItem of(String userId, String ticker) {
        return new WatchlistItem(userId, ticker, OffsetDateTime.now());
    }
}
