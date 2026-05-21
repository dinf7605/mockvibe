package com.fintech.simulator.ai;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini API 호출 (PRD §9.4 Claude 대체, 영구 무료 티어).
 *
 * - POST /v1beta/models/{model}:generateContent?key={KEY}
 * - 시스템 인스트럭션 + user prompt 분리 → Gemini가 system을 별도 캐시·인식
 * - usageMetadata로 입출력 토큰 수 측정 → Micrometer 메트릭
 * - Resilience4j Circuit Breaker(D25 `claude` 인스턴스 재사용)로 외부 장애 격리
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.external.gemini.api-key")
public class GeminiClient {

    private final GeminiProperties props;
    private final RestClient restClient;
    private final MeterRegistry meterRegistry;

    private Counter inputTokens;
    private Counter outputTokens;
    private Counter callCount;

    public GeminiClient(GeminiProperties props, MeterRegistry meterRegistry) {
        this.props = props;
        this.meterRegistry = meterRegistry;
        this.restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    @PostConstruct
    void init() {
        this.inputTokens  = Counter.builder("ai.tokens.input").register(meterRegistry);
        this.outputTokens = Counter.builder("ai.tokens.output").register(meterRegistry);
        this.callCount    = Counter.builder("ai.call.count").register(meterRegistry);
    }

    /**
     * @return [응답 텍스트, 총 토큰 수]
     */
    /** 메인 모델 (D38 매매 코멘트, D40 즉시 분석). 가장 여유로운 무료 한도. */
    public GeminiResult generate(String systemPrompt, String userPrompt) {
        return invoke(props.mainModel(), props.maxOutputTokens(), systemPrompt, userPrompt);
    }

    /** 주간 회고 (D39) — 더 똑똑한 모델, 한도 적음. 사용자별 주1회만 호출. */
    public GeminiResult generateWeekly(String systemPrompt, String userPrompt) {
        return invoke(props.weeklyModel(), props.weeklyMaxOutputTokens(), systemPrompt, userPrompt);
    }

    @CircuitBreaker(name = "claude", fallbackMethod = "invokeFallback")
    public GeminiResult invoke(String model, int maxTokens, String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents",          List.of(Map.of("parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig",  Map.of(
                        "maxOutputTokens", maxTokens,
                        "temperature", 0.7
                )
        );
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String text = extractText(res);
            int totalTokens = extractTokens(res);
            recordMetrics(res, totalTokens);
            callCount.increment();
            return new GeminiResult(text, totalTokens);
        } catch (RestClientException e) {
            log.warn("Gemini API call failed ({}): {}", model, e.getMessage());
            throw new BusinessException(ErrorCode.AI_API_ERROR, e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public GeminiResult invokeFallback(String model, int maxTokens,
                                       String sys, String user, Throwable t) {
        log.warn("Gemini CB fallback ({}): {}", model, t.getMessage());
        throw new BusinessException(ErrorCode.AI_API_ERROR, "AI 코치 서비스가 일시적으로 응답할 수 없습니다.");
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> res) {
        if (res == null) return "(빈 응답)";
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) res.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "(응답 없음)";
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) return "(응답 없음)";
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "(응답 없음)";
        Object text = parts.get(0).get("text");
        return text == null ? "(응답 없음)" : text.toString();
    }

    @SuppressWarnings("unchecked")
    private int extractTokens(Map<String, Object> res) {
        if (res == null) return 0;
        Map<String, Object> usage = (Map<String, Object>) res.get("usageMetadata");
        if (usage == null) return 0;
        Object total = usage.get("totalTokenCount");
        return total instanceof Number n ? n.intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    private void recordMetrics(Map<String, Object> res, int total) {
        if (res == null) return;
        Map<String, Object> usage = (Map<String, Object>) res.get("usageMetadata");
        if (usage == null) return;
        Object in  = usage.get("promptTokenCount");
        Object out = usage.get("candidatesTokenCount");
        if (in  instanceof Number n) inputTokens.increment(n.doubleValue());
        if (out instanceof Number n) outputTokens.increment(n.doubleValue());
    }

    public record GeminiResult(String text, int totalTokens) {}
}
