package com.finmate.learning;

import com.finmate.common.ApiException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** 학습 개념과 AI를 연결하는 오케스트레이터. 콘텐츠 조회와 생성기 선택을 한 곳에서 관리한다. */
@Service
public class LearningAiService {

    private static final int MAX_QUESTION = 300;
    private static final int MAX_ANSWER = 1_200;
    private static final Pattern DISALLOWED = Pattern.compile(
            "매수|매도|가입하|구매하|추천|권유|보장|확실한?\\s*수익|수익률.{0,12}(예상|예측)",
            Pattern.CASE_INSENSITIVE);

    private final LearningCatalog catalog;
    private final ClaudeLearningTutor claude;

    public LearningAiService(LearningCatalog catalog, ClaudeLearningTutor claude) {
        this.catalog = catalog;
        this.claude = claude;
    }

    public LearningAiDtos.ExplainResponse explain(String slug, LearningAiDtos.ExplainRequest request) {
        LearningContent.Node node = require(slug);
        String question = request == null || request.question() == null || request.question().isBlank()
                ? "이 개념을 금융을 처음 배우는 사람에게 쉽게 설명해 주세요."
                : request.question().trim();
        if (question.length() > MAX_QUESTION) {
            throw ApiException.badRequest("질문은 300자 이내로 입력해 주세요.");
        }

        String answer;
        String source;
        if (claude.isEnabled()) {
            try {
                answer = claude.generate(buildPrompt(node, question));
                if (valid(answer)) {
                    source = "CLAUDE";
                } else {
                    answer = template(node);
                    source = "TEMPLATE";
                }
            } catch (RuntimeException ignored) {
                answer = template(node);
                source = "TEMPLATE";
            }
        } else {
            answer = template(node);
            source = "TEMPLATE";
        }
        return new LearningAiDtos.ExplainResponse(
                node.slug(), node.title(), answer, source,
                node.sourceName(), node.sourceUrl(), catalog.version(), LearningAiDtos.NOTICE);
    }

    private LearningContent.Node require(String slug) {
        return catalog.lookup(slug)
                .orElseThrow(() -> ApiException.notFound("학습 개념을 찾을 수 없습니다: " + slug));
    }

    private String buildPrompt(LearningContent.Node node, String question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("[승인된 학습 콘텐츠]\n")
                .append("개념명: ").append(node.title()).append("\n")
                .append("한 줄 정의: ").append(node.oneLiner()).append("\n")
                .append("요약: ").append(node.summary()).append("\n")
                .append("설명:\n").append(String.join("\n", node.explanation())).append("\n")
                .append("확인 포인트:\n").append(String.join("\n", node.keyPoints())).append("\n")
                .append("자주 하는 오해: ").append(node.misconception()).append("\n")
                .append("출처: ").append(node.sourceName()).append(" (콘텐츠 버전 ")
                .append(catalog.version()).append(")\n\n")
                .append("[사용자 질문]\n").append(question);
        return prompt.toString();
    }

    private boolean valid(String answer) {
        return answer != null && !answer.isBlank()
                && answer.codePointCount(0, answer.length()) <= MAX_ANSWER
                && !DISALLOWED.matcher(answer).find();
    }

    private String template(LearningContent.Node node) {
        String points = node.keyPoints().isEmpty() ? "" : " 확인할 점은 " + String.join(", ", node.keyPoints()) + "입니다.";
        return node.oneLiner() + points + " 자세한 내용은 " + node.sourceName() + " 원문에서 확인할 수 있습니다.";
    }
}
