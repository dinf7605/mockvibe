package com.fintech.simulator.ai.prompt;

import com.fintech.simulator.portfolio.dto.PortfolioResponse;
import com.fintech.simulator.portfolio.dto.PortfolioResponse.HoldingItem;
import com.fintech.simulator.portfolio.dto.PortfolioResponse.RegionShare;
import java.util.Collections;
import com.fintech.simulator.trading.domain.OrderSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder pb = new PromptBuilder();

    private PortfolioResponse pf(BigDecimal total, BigDecimal pnl, List<HoldingItem> holdings) {
        return new PortfolioResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, total, BigDecimal.ZERO, pnl, BigDecimal.ZERO,
                holdings, new RegionShare(BigDecimal.valueOf(60), BigDecimal.valueOf(30), BigDecimal.valueOf(10))
        );
    }

    @Test
    @DisplayName("system 프롬프트는 한국어 + 1~3문장 규칙 포함")
    void system_includes_rules() {
        assertThat(pb.system()).contains("한국어").contains("1~3문장");
    }

    @Test
    @DisplayName("tradeCommentPrompt에 사용자명/종목/매수매도 포함")
    void trade_prompt() {
        String p = pb.tradeCommentPrompt("길동", OrderSide.BUY, "005930",
                new BigDecimal("78000"), BigDecimal.ONE, new BigDecimal("78000"));
        assertThat(p).contains("길동").contains("005930").contains("매수");
    }

    @Test
    @DisplayName("portfolioHash는 보유 종목·수량 기반. 시세 변동(totalAsset)에 무관.")
    void hash_uses_holdings_only() {
        HoldingItem h1 = new HoldingItem("AAPL", "Apple", "NASDAQ", "USD",
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        HoldingItem h2 = new HoldingItem("AAPL", "Apple", "NASDAQ", "USD",
                new BigDecimal("20"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        // 같은 보유 + 다른 평가금액 → 같은 hash
        PortfolioResponse a = pf(new BigDecimal("10000000"), new BigDecimal("100"), List.of(h1));
        PortfolioResponse b = pf(new BigDecimal("11000000"), new BigDecimal("500"), List.of(h1));
        assertThat(pb.portfolioHash(a)).isEqualTo(pb.portfolioHash(b));

        // 보유 수량 변경 → 다른 hash
        PortfolioResponse c = pf(new BigDecimal("10000000"), new BigDecimal("100"), List.of(h2));
        assertThat(pb.portfolioHash(a)).isNotEqualTo(pb.portfolioHash(c));

        // 빈 포트폴리오는 빈 입력 hash
        PortfolioResponse empty = pf(BigDecimal.ZERO, BigDecimal.ZERO, Collections.emptyList());
        assertThat(pb.portfolioHash(empty)).hasSize(64);
    }
}
