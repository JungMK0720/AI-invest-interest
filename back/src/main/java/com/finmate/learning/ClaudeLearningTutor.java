package com.finmate.learning;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.TextBlock;
import java.time.Duration;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 승인된 개념 원문을 근거로 초보자 질문에 답하는 생성기. */
@Component
public class ClaudeLearningTutor {

    private static final String SYSTEM_PROMPT = """
            당신은 금융상품 학습 서비스의 튜터입니다.
            제공된 [승인된 학습 콘텐츠] 안에서만 설명하고, 모르는 내용은 모른다고 말하세요.
            투자 성향 점수·등급·추천을 만들거나 바꾸지 마세요.
            특정 종목의 매수·매도, 수익률 예측·보장, 개인별 투자 조언을 하지 마세요.
            한국어 존댓말로 핵심부터 설명하고, 필요하면 짧은 예시를 드세요. 900자 이내로 답하세요.
            """;

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;

    public ClaudeLearningTutor(
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${finmate.analysis.claude.model:claude-opus-5}") String model,
            @Value("${finmate.learning.ai.max-tokens:1200}") long maxTokens,
            @Value("${finmate.analysis.claude.timeout-seconds:30}") long timeoutSeconds) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.client = apiKey.isBlank() ? null : AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public boolean isEnabled() {
        return client != null;
    }

    public String generate(String prompt) {
        if (client == null) {
            throw new IllegalStateException("ANTHROPIC_API_KEY 가 설정되지 않았습니다.");
        }
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(SYSTEM_PROMPT)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .addUserMessage(prompt)
                .build();
        Message response = client.messages().create(params);
        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining("\n"))
                .trim();
        if (text.isEmpty()) {
            throw new IllegalStateException("AI 학습 설명이 비어 있습니다. stopReason=" + response.stopReason());
        }
        return text;
    }
}
