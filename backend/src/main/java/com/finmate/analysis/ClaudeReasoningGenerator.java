package com.finmate.analysis;

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

/**
 * Claude Messages API로 성향 판정의 근거 문장을 생성한다.
 *
 * 이 생성기는 "설명"만 담당한다. 성향 등급과 탐색 카테고리는 이미 규칙 엔진이 확정한 뒤 넘어오며,
 * 프롬프트에서도 등급 변경을 금지한다. 모델이 규제 대상 판단을 흔들 수 없게 하기 위한 경계다.
 */
@Component
public class ClaudeReasoningGenerator {

    private static final String SYSTEM_PROMPT = """
            당신은 투자자 성향 진단 결과를 사용자에게 설명하는 금융 서비스의 설명 생성기입니다.

            반드시 지켜야 할 규칙:
            - 투자 성향 등급은 이미 규칙 엔진이 확정했습니다. 등급을 바꾸거나 다른 등급을 제안하지 마세요.
            - 사용자가 실제로 선택한 답변만 근거로 사용하고, 주어지지 않은 사실을 지어내지 마세요.
            - 특정 종목명이나 실제 판매 중인 상품명을 언급하지 마세요.
            - 수익률을 예측하거나 보장하는 표현을 쓰지 마세요.
            - 한국어 존댓말로 2~3문장, 200자 이내로 작성하세요.
            - 머리말, 목록 기호, 따옴표 없이 설명 문장만 출력하세요.
            """;

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;

    public ClaudeReasoningGenerator(
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${finmate.analysis.claude.model:claude-opus-5}") String model,
            @Value("${finmate.analysis.claude.max-tokens:4096}") long maxTokens,
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

    public String generate(ReasoningRequest request) {
        if (client == null) {
            throw new IllegalStateException("ANTHROPIC_API_KEY 가 설정되지 않았습니다.");
        }
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(SYSTEM_PROMPT)
                // 짧은 설명 한 단락이므로 낮은 effort 로 지연과 비용을 줄인다.
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .addUserMessage(buildUserPrompt(request))
                .build();

        Message response = client.messages().create(params);
        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining("\n"))
                .trim();

        if (text.isEmpty()) {
            throw new IllegalStateException("근거 문장이 비어 있습니다. stopReason=" + response.stopReason());
        }
        return text;
    }

    private String buildUserPrompt(ReasoningRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("투자 성향: ")
                .append(request.profileType().label())
                .append(" (5단계 중 ")
                .append(request.profileType().grade())
                .append("등급)\n");
        prompt.append("성향 점수: ").append(request.totalScore()).append("점 (100점 만점)\n\n");

        prompt.append("사용자가 선택한 답변:\n");
        for (ScoredAnswer answer : request.answers()) {
            prompt.append("- ")
                    .append(answer.questionContent())
                    .append(": ")
                    .append(answer.choiceLabel())
                    .append(" (배점 비중 ")
                    .append(answer.weight())
                    .append(")\n");
        }

        if (!request.recommendedCategoryNames().isEmpty()) {
            prompt.append("\n안내된 상품 카테고리: ")
                    .append(String.join(", ", request.recommendedCategoryNames()))
                    .append("\n");
        }

        prompt.append("\n위 답변을 근거로 왜 이 성향으로 분류되었는지 설명해 주세요.");
        return prompt.toString();
    }
}
