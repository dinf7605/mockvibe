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
import java.time.LocalDate;

@Entity
@Table(name = "PRICE_HISTORY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "ticker", length = 20, nullable = false)
    private String ticker;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "open_price",  precision = 18, scale = 4, nullable = false)
    private BigDecimal open;

    @Column(name = "high_price",  precision = 18, scale = 4, nullable = false)
    private BigDecimal high;

    @Column(name = "low_price",   precision = 18, scale = 4, nullable = false)
    private BigDecimal low;

    @Column(name = "close_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal close;

    @Column(name = "volume", nullable = false)
    private Long volume;

    private PriceHistory(String ticker, LocalDate date, BigDecimal o, BigDecimal h, BigDecimal l, BigDecimal c, long vol) {
        this.ticker = ticker; this.tradeDate = date;
        this.open = o; this.high = h; this.low = l; this.close = c; this.volume = vol;
    }

    public static PriceHistory of(String ticker, LocalDate date, BigDecimal o, BigDecimal h, BigDecimal l, BigDecimal c, long vol) {
        return new PriceHistory(ticker, date, o, h, l, c, vol);
    }

    /**
     * 외부 API 일봉 재fetch 시 동일 날짜의 OHLCV 를 덮어쓰기 위한 UPSERT update.
     * (대부분의 KIS 응답은 동일 날짜 값이 stable 하지만, 장중 데이터 정정 가능성 대비)
     */
    public void update(BigDecimal o, BigDecimal h, BigDecimal l, BigDecimal c, long vol) {
        this.open = o;
        this.high = h;
        this.low = l;
        this.close = c;
        this.volume = vol;
    }

    /**
     * 장중 폴링으로 받은 현재가를 오늘 candle 에 누적.
     * high = max, low = min, close = 최신가. open 은 첫 기록값 유지.
     */
    public void applyIntraday(BigDecimal price) {
        if (price == null || price.signum() <= 0) return;
        if (high == null || price.compareTo(high) > 0) this.high = price;
        if (low == null  || price.compareTo(low) < 0)  this.low = price;
        this.close = price;
    }

    /** 오늘 첫 기록 시 OHLC 를 모두 현재가로 초기화하는 팩토리 */
    public static PriceHistory intradayOpen(String ticker, LocalDate date, BigDecimal price) {
        return new PriceHistory(ticker, date, price, price, price, price, 0L);
    }
}
