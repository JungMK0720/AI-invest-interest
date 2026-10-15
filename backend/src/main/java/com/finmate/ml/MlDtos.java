package com.finmate.ml;

import java.util.List;

/** 모델 결과는 설문 판정과 분리된 교육용 보조 정보다. */
public final class MlDtos {

    private MlDtos() {}

    public record StyleCheckRequest(String text, Integer questionnaireGrade) {}

    public record StyleCheckResponse(String status, String estimatedProfileType,
                                     String estimatedProfileLabel, Integer estimatedGrade,
                                     double confidence, int questionnaireGrade, boolean mismatch,
                                     String modelVersion, String notice) {}

    public record TermSearchRequest(String question, Integer limit) {}

    public record TermHit(String slug, String term, double score) {}

    public record TermSearchResponse(List<TermHit> results, String modelVersion) {}
}
