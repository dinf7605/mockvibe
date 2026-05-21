package com.fintech.simulator.market.provider.finnhub;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 `app.external.finnhub.*` 매핑.
 * - api-key가 비어있으면 Finnhub Bean들이 비활성 (env 미주입 환경 안전)
 * - rate-per-minute: REST 호출 한도 (무료 티어 60/min)
 */
@ConfigurationProperties(prefix = "app.external.finnhub")
public record FinnhubProperties(
        String baseUrl,
        String wsUrl,
        String apiKey,
        Integer ratePerMinute
) {
    public FinnhubProperties {
        if (ratePerMinute == null || ratePerMinute <= 0) ratePerMinute = 60;
    }
}
