package com.fintech.simulator.fx;

import com.fintech.simulator.fx.repository.FxRateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ApiFxRateProviderTest {

    @Mock FxRateRepository fxRateRepository;

    @Test
    @DisplayName("baseUrl이 비어있으면 doFetch는 no-op, rate()는 fallback (USD→KRW=1380)")
    void blank_url_uses_fallback() {
        FxProperties props = new FxProperties(null, 60, 60);
        ApiFxRateProvider provider = new ApiFxRateProvider(props, fxRateRepository);
        provider.doFetch();   // 예외 없어야 함

        assertThat(provider.rate("USD", "KRW")).isEqualByComparingTo("1380.0");
        assertThat(provider.rate("KRW", "KRW")).isEqualByComparingTo("1");
        assertThat(provider.rate(null, "KRW")).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("같은 통화면 1, 알 수 없는 통화는 fallback ONE")
    void same_and_unknown() {
        ApiFxRateProvider provider = new ApiFxRateProvider(
                new FxProperties("", 60, 60), fxRateRepository);
        assertThat(provider.rate("USD", "USD")).isEqualByComparingTo("1");
        assertThat(provider.rate("USD", "EUR")).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("FxProperties 기본값 60초")
    void properties_defaults() {
        FxProperties props = new FxProperties(null, null, null);
        assertThat(props.refreshIntervalSeconds()).isEqualTo(60);
        assertThat(props.cacheTtlSeconds()).isEqualTo(60);
    }
}
