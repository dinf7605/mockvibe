package com.fintech.simulator.ai.service;

import com.fintech.simulator.ai.AiDailyLimiter;
import com.fintech.simulator.ai.GeminiClient;
import com.fintech.simulator.ai.GeminiClient.GeminiResult;
import com.fintech.simulator.ai.domain.AiReport;
import com.fintech.simulator.ai.domain.AiReportType;
import com.fintech.simulator.ai.prompt.PromptBuilder;
import com.fintech.simulator.ai.repository.AiReportRepository;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.portfolio.dto.PortfolioResponse;
import com.fintech.simulator.portfolio.service.PortfolioService;
import com.fintech.simulator.trading.domain.OrderSide;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * AI 코치 통합 서비스.
 *
 * - 매매 직후 한 줄 코멘트 (TRADE_COMMENT, 트리거 제한 적용)
 * - 사용자 명시 요청 즉시 분석 (INSTANT, 응답 캐싱 24h)
 * - 주간 회고 (WEEKLY, 스케줄러에서 호출)
 *
 * GeminiClient가 Bean으로 등록되지 않은 환경(키 미설정)에서는 ObjectProvider로 안전 조회 → 빈 응답 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCoachService {

    private static final int CACHE_HOURS = 24;

    private final ObjectProvider<GeminiClient> geminiClientProvider;
    private final PromptBuilder promptBuilder;
    private final AiDailyLimiter dailyLimiter;
    private final AiReportRepository aiReportRepository;
    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    /** 매매 직후 한 줄 코멘트 (호출 한도 적용, 실패해도 매매는 영향 X — 호출자가 비동기로 호출) */
    @Transactional
    public Optional<AiReport> commentOnTrade(String userId, OrderSide side, String ticker,
                                             BigDecimal price, BigDecimal quantity, BigDecimal totalKrw) {
        GeminiClient client = geminiClientProvider.getIfAvailable();
        if (client == null) return Optional.empty();
        String username = usernameOf(userId);
        try {
            dailyLimiter.acquire(userId);
        } catch (Exception e) {
            log.debug("Skip TRADE_COMMENT: {}", e.getMessage());
            return Optional.empty();
        }
        String prompt = promptBuilder.tradeCommentPrompt(username, side, ticker, price, quantity, totalKrw);
        GeminiResult r = client.generate(promptBuilder.system(), prompt);
        return Optional.of(aiReportRepository.save(
                AiReport.of(userId, AiReportType.TRADE_COMMENT, null, r.text(), r.totalTokens())));
    }

    /** 즉시 분석 + 응답 캐싱 (portfolio hash 동일 + 24h 이내면 재사용, API 호출 0) */
    @Transactional
    public AiReport analyzeInstant(String userId) {
        GeminiClient client = geminiClientProvider.getIfAvailable();
        if (client == null) {
            return aiReportRepository.save(AiReport.of(userId, AiReportType.INSTANT, null,
                    "AI 코치가 비활성 상태입니다. (GEMINI_API_KEY 미설정)", 0));
        }
        PortfolioResponse pf = portfolioService.get(userId);
        String hash = promptBuilder.portfolioHash(pf);

        Optional<AiReport> cached = aiReportRepository.findFirstByUserIdAndContextHashAndCreatedAtAfter(
                userId, hash, OffsetDateTime.now().minusHours(CACHE_HOURS));
        if (cached.isPresent()) {
            log.debug("AI cache hit: user={} hash={}", userId, hash);
            return cached.get();
        }

        dailyLimiter.acquire(userId);
        String prompt = promptBuilder.instantPrompt(usernameOf(userId), pf);
        GeminiResult r = client.generate(promptBuilder.system(), prompt);
        return aiReportRepository.save(
                AiReport.of(userId, AiReportType.INSTANT, hash, r.text(), r.totalTokens()));
    }

    /** 주간 회고 (D39 스케줄러가 호출). 더 똑똑한 weeklyModel 사용 — 하루 한도 적음. */
    @Transactional
    public Optional<AiReport> weekly(String userId, long buyCount, long sellCount) {
        GeminiClient client = geminiClientProvider.getIfAvailable();
        if (client == null) return Optional.empty();
        PortfolioResponse pf = portfolioService.get(userId);
        String prompt = promptBuilder.weeklyPrompt(usernameOf(userId), pf, buyCount, sellCount);
        GeminiResult r = client.generateWeekly(promptBuilder.system(), prompt);
        return Optional.of(aiReportRepository.save(
                AiReport.of(userId, AiReportType.WEEKLY, null, r.text(), r.totalTokens())));
    }

    private String usernameOf(String userId) {
        return userRepository.findById(userId).map(u -> u.getUsername()).orElse("사용자");
    }
}
