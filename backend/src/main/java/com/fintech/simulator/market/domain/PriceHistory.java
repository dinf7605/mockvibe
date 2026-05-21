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
}
