package com.fintech.simulator.market.provider.kis;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.provider.Quote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisTest {

    // ===== Parser =====

    @Test
    @DisplayName("Parser: H0STCNT0 정상 메시지 → ticker + price")
    void parse_stock_exec() {
        String raw = "0|H0STCNT0|001|005930^123456^78500^150^+200";
        Optional<Quote> q = KisQuoteMessageParser.parse(raw);
        assertThat(q).isPresent();
        assertThat(q.get().ticker()).isEqualTo("005930");
        assertThat(q.get().price()).isEqualByComparingTo("78500");
    }

    @Test
    @DisplayName("Parser: 다른 tr_id면 empty")
    void parse_other_tr_id() {
        assertThat(KisQuoteMessageParser.parse("0|H0STASP0|001|005930^...")).isEmpty();
    }

    @Test
    @DisplayName("Parser: 깨진 메시지 → empty (예외 없음)")
    void parse_corrupted() {
        assertThat(KisQuoteMessageParser.parse(null)).isEmpty();
        assertThat(KisQuoteMessageParser.parse("")).isEmpty();
        assertThat(KisQuoteMessageParser.parse("0|H0STCNT0|001|005930^t^NOT_NUMERIC")).isEmpty();
    }

    // ===== SubscriptionManager =====

    @Test
    @DisplayName("SubscriptionManager: 한도 초과 시 SUBSCRIPTION_LIMIT, 멱등 subscribe")
    void subscribe_limit_and_idempotent() {
        KisProperties props = new KisProperties("u", "ws", "k", "s", true, 3);
        KisSubscriptionManager mgr = new KisSubscriptionManager(props);

        assertThat(mgr.subscribe("A")).isTrue();
        assertThat(mgr.subscribe("A")).isFalse();  // 이미 있음 → false
        assertThat(mgr.subscribe("B")).isTrue();
        assertThat(mgr.subscribe("C")).isTrue();
        assertThat(mgr.size()).isEqualTo(3);

        assertThatThrownBy(() -> mgr.subscribe("D"))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SUBSCRIPTION_LIMIT);

        assertThat(mgr.unsubscribe("B")).isTrue();
        assertThat(mgr.unsubscribe("B")).isFalse();
        assertThat(mgr.subscribe("D")).isTrue();
    }

    @Test
    @DisplayName("KisProperties: maxSubscriptions null/0 → 기본값 41")
    void properties_default() {
        assertThat(new KisProperties("u", "ws", "k", "s", true, null).maxSubscriptions()).isEqualTo(41);
        assertThat(new KisProperties("u", "ws", "k", "s", true, 0).maxSubscriptions()).isEqualTo(41);
    }
}
