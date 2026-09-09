package com.finmate.assessment;

import com.finmate.analysis.ScoredAnswer;
import java.util.List;

/** 진단 당시의 입력과 결정. 현재 문항/카테고리에서 다시 조립하지 않는다. */
public record DecisionSnapshot(
        String ruleVersion, String questionnaireVersion,
        List<AnswerEvidence> answers,
        List<AssessmentDtos.CategoryRecommendationResponse> categories,
        List<ReviewFlag> reviewFlags) {
    public static final String RULE_VERSION = "finmate-exploration-v1.1";
    public record AnswerEvidence(String questionId, String choiceId, ScoredAnswer value) {}
    public record ReviewFlag(String code, String message) {}
}
