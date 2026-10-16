package com.finmate.ml;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/** 모델 결과는 설문 판정과 분리된 교육용 보조 정보다. */
public final class MlDtos {

    private MlDtos() {}

    public record StyleCheckRequest(String text, Integer questionnaireGrade) {}

    /** 관심사 태그 하나. score는 태그별 로지스틱 회귀의 확률이다. */
    public record TagSignal(String code, String label, double score) {}

    /** 태그가 안내하는 다음 학습. 개념·용어 slug만 담고 화면이 제목을 붙인다. */
    public record LearningLink(String code, String label, String message,
                               List<String> conceptSlugs, List<String> termSlugs) {}

    public record StyleCheckResponse(String status, String estimatedProfileType,
                                     String estimatedProfileLabel, Integer estimatedGrade,
                                     double confidence, int questionnaireGrade, boolean mismatch,
                                     Map<String, Double> probabilities, List<TagSignal> tags,
                                     List<LearningLink> learningLinks,
                                     String modelVersion, String notice) {}

    public record TermSearchRequest(String question, Integer limit) {}

    /** nameMatched는 질문에 용어명이 그대로 들어 있어 가산점을 받았는지를 뜻한다. */
    public record TermHit(String slug, String term, String categoryId, String definition,
                          double score, boolean nameMatched) {}

    public record TermSearchResponse(List<TermHit> results, String modelVersion, int indexedTerms) {}

    public record ModelsResponse(JsonNode card, Map<String, Object> runtime) {}
}
