package com.fintech.simulator.market.provider.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 `app.external.kis.*` 매핑.
 */
@ConfigurationProperties(prefix = "app.external.kis")
public record KisProperties(
        String baseUrl,
        String wsUrl,
        String appKey,
        String appSecret,
        boolean mock,
        Integer maxSubscriptions
) {
    public KisProperties {
        if (maxSubscriptions == null || maxSubscriptions <= 0) maxSubscriptions = 41;
    }
}
