package com.fintech.simulator.fx;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 `app.external.fx.*` 매핑.
 * - base-url: ExchangeRate-API (혹은 ECOS)
 * - refresh-interval-seconds: 백그라운드 갱신 주기 (PRD FR-6.2: 1분)
 * - cache-ttl-seconds: 메모리 캐시 TTL (스케줄러와 같거나 약간 짧게)
 */
@ConfigurationProperties(prefix = "app.external.fx")
public record FxProperties(
        String baseUrl,
        Integer refreshIntervalSeconds,
        Integer cacheTtlSeconds
) {
    public FxProperties {
        if (refreshIntervalSeconds == null || refreshIntervalSeconds <= 0) refreshIntervalSeconds = 60;
        if (cacheTtlSeconds == null || cacheTtlSeconds <= 0) cacheTtlSeconds = 60;
    }
}
