package com.fintech.simulator.market.provider.kis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * KIS REST 공통 호출 래퍼.
 * - Bearer 토큰 + appkey/appsecret 헤더 자동 부착
 * - tr_id는 호출자가 명시 (KIS 거래 ID — 시세/주문별로 상이)
 * - Rate Limiter 통과 후 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisRestClient {

    private final KisProperties props;
    private final KisAuthService authService;
    private final KisRateLimiter rateLimiter;

    public <T> T get(String path, String trId, Class<T> responseType) {
        rateLimiter.acquire();
        return restClient().get()
                .uri(path)
                .headers(h -> applyHeaders(h, trId))
                .retrieve()
                .body(responseType);
    }

    private RestClient restClient() {
        return RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    private void applyHeaders(HttpHeaders h, String trId) {
        h.setBearerAuth(authService.getAccessToken());
        h.set("appkey",    props.appKey());
        h.set("appsecret", props.appSecret());
        h.set("tr_id",     trId);
    }
}
