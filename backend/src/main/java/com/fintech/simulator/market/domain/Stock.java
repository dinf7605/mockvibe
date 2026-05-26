package com.fintech.simulator.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "STOCKS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @Column(name = "ticker", length = 20)
    private String ticker;

    @Column(name = "market", length = 10, nullable = false)
    private String market;       // KRX | NASDAQ | NYSE

    @Column(name = "currency", length = 10, nullable = false)
    private String currency;     // KRW | USD

    @Column(name = "company_name", length = 100, nullable = false)
    private String companyName;

    @Column(name = "sector", length = 50)
    private String sector;

    @Column(name = "region", length = 10)
    private String region;       // KR | US

    @Column(name = "current_price", precision = 18, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "tick_size", precision = 18, scale = 4, nullable = false)
    private BigDecimal tickSize;

    @Column(name = "is_active", nullable = false)
    private Integer isActive;    // 0 / 1

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public boolean isActive() { return isActive != null && isActive == 1; }

    public void toggleActive() {
        this.isActive = isActive() ? 0 : 1;
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * STOCKS.current_price 갱신.
     * - PriceHistorySeeder 가 시드 후 마지막 close 로 동기화할 때 사용
     * - 향후 Provider 가 실시간 가격을 STOCKS 에 sync 할 때도 사용 가능
     */
    public void setCurrentPrice(BigDecimal price) {
        this.currentPrice = price;
        this.updatedAt = OffsetDateTime.now();
    }
}
