package com.fintech.simulator.ai.prompt;

import com.fintech.simulator.portfolio.dto.PortfolioResponse;
import com.fintech.simulator.trading.domain.OrderSide;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * AI 프롬프트 생성.
 *
 * 시스템 프롬프트는 캐싱 친화적으로 분리(모든 호출 공통).
 * 사용자 프롬프트는 호출별 컨텍스트(매매·포트폴리오)만 포함.
 *
 * PRD FR-7.4 프롬프트 캐싱:
 *   Gemini는 동일 systemInstruction에 대해 내부 캐시 활용 → 비용/지연 절감.
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM = """
            당신은 한국·미국 주식 모의투자 사용자에게 친절하지만 냉정한 매매 코치입니다.
            규칙:
              1. 반드시 한국어 존댓말로 1~3문장.
              2. 투자 권유나 단정적 예측은 금지(예: "오를 것입니다" X).
              3. 객관적 리스크·분산·평균단가 관점으로 짧게 조언.
              4. 이모지 1개 이내.
              5. 종목명을 짧게 인용하되 회사 분석은 깊게 들어가지 않습니다.
            """;

    public String system() { return SYSTEM; }

    public String tradeCommentPrompt(String username, OrderSide side, String ticker,
                                     BigDecimal price, BigDecimal quantity, BigDecimal totalKrw) {
        return """
                사용자: %s 님이 %s 종목을 %s 주문 체결했습니다.
                - 체결가: %s
                - 수량: %s
                - 체결 총액(KRW): %s
                위 매매에 대해 1~2문장으로 코멘트해 주세요.
                """.formatted(
                username, ticker,
                side == OrderSide.BUY ? "매수" : "매도",
                price, quantity, totalKrw
        );
    }

    public String weeklyPrompt(String username, PortfolioResponse pf, long buyCount, long sellCount) {
        return """
                %s 님의 지난 주 매매 회고를 부탁드립니다.
                - 총자산(KRW): %s
                - 평가 손익: %s (%s%%)
                - 보유 종목 수: %d
                - 한국/미국 비중: %s%% / %s%%
                - 매수 %d건, 매도 %d건
                강점·약점·다음 주 주의점을 3문장 이내로.
                """.formatted(
                username, pf.totalAssetKrw(), pf.totalPnlKrw(), pf.totalPnlPct(),
                pf.holdings().size(),
                pf.regionShare().kr(), pf.regionShare().us(),
                buyCount, sellCount
        );
    }

    public String instantPrompt(String username, PortfolioResponse pf) {
        return """
                %s 님의 현재 포트폴리오 즉시 분석을 부탁드립니다.
                - 총자산(KRW): %s
                - 평가 손익: %s (%s%%)
                - 보유 종목 수: %d
                - 한국/미국/예수금 비중: %s%% / %s%% / %s%%
                집중도 위험·통화 분산·평균단가 관점에서 2~3문장 코멘트.
                """.formatted(
                username, pf.totalAssetKrw(), pf.totalPnlKrw(), pf.totalPnlPct(),
                pf.holdings().size(),
                pf.regionShare().kr(), pf.regionShare().us(), pf.regionShare().cash()
        );
    }

    /**
     * 응답 캐싱 키 — 보유 종목·수량만으로 해시.
     * 시세 변동(totalAssetKrw)에는 무관하게 같은 포지션이면 24h 캐시 hit.
     * 매매가 일어나 holdings가 바뀌면 hash 변경 → 자동 무효화.
     */
    public String portfolioHash(PortfolioResponse pf) {
        StringBuilder sb = new StringBuilder();
        pf.holdings().stream()
                .sorted((a, b) -> a.ticker().compareTo(b.ticker()))
                .forEach(h -> sb.append(h.ticker()).append(':').append(h.quantity()).append(','));
        return sha256(sb.toString());
    }

    private static String sha256(String s) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(s.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
