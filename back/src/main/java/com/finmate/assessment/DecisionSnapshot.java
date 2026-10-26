package com.finmate.assessment;

import com.finmate.analysis.ScoredAnswer;
import java.util.List;

/**
 * 진단 당시의 입력과 결정. 현재 문항/카테고리에서 다시 조립하지 않는다.
 *
 * @param narrative 자유 서술 모델 추정. 입력하지 않았거나 이전 버전 진단이면 null
 */
public record DecisionSnapshot(
        String ruleVersion, String questionnaireVersion,
        List<AnswerEvidence> answers,
        List<AssessmentDtos.CategoryRecommendationResponse> categories,
        List<ReviewFlag> reviewFlags,
        NarrativeSignal narrative) {
    public static final String RULE_VERSION = "finmate-exploration-v1.1";
    public record AnswerEvidence(String questionId, String choiceId, ScoredAnswer value) {}
    public record ReviewFlag(String code, String message) {}

    /** 원문 대신 남기는 추정 결과. 같은 모델 버전이면 같은 문장에 같은 값이 나온다. */
    public record NarrativeSignal(String status, String profileType, double confidence, boolean mismatch,
                                  List<NarrativeTag> tags, String modelVersion) {}
    public record NarrativeTag(String code, String label, double score) {}
}
