package com.fintech.simulator.fx.domain;

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
 * 환율 시계열 스냅샷 (1분마다 INSERT).
 * 최신 환율은 (base, quote, fetched_at DESC) 인덱스로 1건 조회.
 */
@Entity
@Table(name = "FX_RATES")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FxRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fx_id")
    private Long fxId;

    @Column(name = "base_currency", length = 10, nullable = false)
    private String baseCurrency;

    @Column(name = "quote_currency", length = 10, nullable = false)
    private String quoteCurrency;

    @Column(name = "rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal rate;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    private FxRate(String base, String quote, BigDecimal rate) {
        this.baseCurrency = base;
        this.quoteCurrency = quote;
        this.rate = rate;
        this.fetchedAt = OffsetDateTime.now();
    }

    public static FxRate snapshot(String base, String quote, BigDecimal rate) {
        return new FxRate(base, quote, rate);
    }
}
