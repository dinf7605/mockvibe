package com.fintech.simulator.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gemini 모델 구성 (PRD §10 #8 비용 통제):
 * - mainModel:   매매 코멘트(D38) + 즉시 분석(D40) — 하루 500회 무료 한도 모델
 * - weeklyModel: 주간 회고(D39) — 더 똑똑하지만 한도가 적은(20~250/일) 모델
 */
@ConfigurationProperties(prefix = "app.external.gemini")
public record GeminiProperties(
        String baseUrl,
        String apiKey,
        String mainModel,
        String weeklyModel,
        Integer maxOutputTokens,
        Integer weeklyMaxOutputTokens,
        Integer dailyCallLimitPerUser
) {
    public GeminiProperties {
        if (mainModel   == null || mainModel.isBlank())   mainModel   = "gemini-3.1-flash-lite";
        if (weeklyModel == null || weeklyModel.isBlank()) weeklyModel = "gemini-3.5-flash";
        if (maxOutputTokens == null       || maxOutputTokens <= 0)       maxOutputTokens = 300;
        if (weeklyMaxOutputTokens == null || weeklyMaxOutputTokens <= 0) weeklyMaxOutputTokens = 800;
        if (dailyCallLimitPerUser == null || dailyCallLimitPerUser <= 0) dailyCallLimitPerUser = 10;
    }
}
