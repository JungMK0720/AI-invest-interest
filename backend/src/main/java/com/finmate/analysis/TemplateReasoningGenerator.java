package com.finmate.analysis;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * AI 호출이 불가능하거나 실패했을 때 사용하는 결정적 근거 생성기.
 * 배점 비중이 큰 문항의 답변을 골라 문장으로 조립하므로 외부 의존성 없이 항상 동작한다.
 */
@Component
public class TemplateReasoningGenerator {

    private static final int HIGHLIGHT_COUNT = 2;

    public String generate(ReasoningRequest request) {
        List<ScoredAnswer> highlights = request.answers().stream()
                .sorted(Comparator.comparingInt(ScoredAnswer::weight).reversed())
                .limit(HIGHLIGHT_COUNT)
                .toList();

        // 선택지 문구는 끝 글자가 제각각이라 조사를 붙이면 어색해진다. 조사가 필요 없는 형태로 조립한다.
        String basis = highlights.stream()
                .map(answer -> "'" + answer.choiceLabel() + "'")
                .reduce((a, b) -> a + ", " + b)
                .orElse("응답하신 내용");

        return "주요 답변: %s. 이 답변을 기준으로 성향 점수 %d점이 산출되어 %s으로 분류되었습니다. 내부 탐색 기준은 위험등급 %d등급 이상입니다."
                .formatted(
                        basis,
                        request.totalScore(),
                        request.profileType().label(),
                        request.profileType().grade());
    }
}
