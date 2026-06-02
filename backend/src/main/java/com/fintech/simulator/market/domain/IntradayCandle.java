package com.fintech.simulator.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 분봉 캔들. (ticker, bucket_ts) UNIQUE — 분 단위 버킷 1행.
 * 분당 폴링 시세를 받아 같은 분이면 누적(high/low/close), 새 분이면 OHLC 초기화.
 */
@Entity
@Table(name = "INTRADAY_CANDLE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntradayCandle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "candle_id")
    private Long candleId;

    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    @Column(name = "bucket_ts", nullable = false)
    private OffsetDateTime bucketTs;

    @Column(name = "open_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal open;

    @Column(name = "high_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal high;

    @Column(name = "low_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal low;

    @Column(name = "close_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal close;

    @Column(name = "volume", nullable = false)
    private Long volume;

    private IntradayCandle(String ticker, OffsetDateTime bucketTs, BigDecimal price) {
        this.ticker = ticker;
        this.bucketTs = bucketTs;
        this.open = price;
        this.high = price;
        this.low = price;
        this.close = price;
        this.volume = 0L;
    }

    /** 새 분 버킷 — OHLC 를 현재가로 초기화. */
    public static IntradayCandle openOf(String ticker, OffsetDateTime bucketTs, BigDecimal price) {
        return new IntradayCandle(ticker, bucketTs, price);
    }

    /** 같은 분 안의 후속 틱 — high/low/close 갱신. */
    public void applyTick(BigDecimal price) {
        if (price == null || price.signum() <= 0) return;
        if (price.compareTo(high) > 0) this.high = price;
        if (price.compareTo(low) < 0) this.low = price;
        this.close = price;
    }
}
