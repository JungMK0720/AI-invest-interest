package com.finmate.analysis;

/**
 * 성향 점수 계산에 필요한 답변 1건.
 * weight 는 문항 배점 비중, score 는 선택지의 0~100 원점수다.
 */
public record ScoredAnswer(
        String questionContent,
        int weight,
        String choiceLabel,
        String analysisKey,
        int score
) {
}
