package com.finmate.learning;

import java.time.Instant;
import java.util.List;

/** 개인화 학습 경로 응답. 상품 추천이 아니라 "다음에 무엇을 배우면 좋은가"의 순서다. */
public final class LearningPathDtos {

    public static final String NOTICE =
            "진단 결과·직접 쓴 생각·오답 기록을 바탕으로 한 학습 순서 안내입니다. 상품 가입이나 매매를 권유하지 않습니다.";

    private LearningPathDtos() { }

    public record NarrativeTag(String code, String label) { }

    /**
     * 경로를 만든 근거. 진단이 없으면 null이다.
     *
     * @param reviewFlags 진단 확인 플래그 코드(예: SHORT_HORIZON, NARRATIVE_MISMATCH)
     */
    public record Basis(String assessmentId, String profileType, String profileLabel, Instant assessedAt,
                        List<String> reviewFlags, List<NarrativeTag> narrativeTags, boolean narrativeMismatch) { }

    /**
     * 경로의 한 걸음.
     *
     * @param kind       REVIEW(복습), CONCEPT(학습 개념), TERM(용어)
     * @param target     CONCEPT 또는 TERM. 화면이 어느 페이지로 보낼지 결정한다.
     * @param reasonCode 어떤 신호에서 나왔는지(태그·플래그 코드, PROFILE, DUE_REVIEW, STARTER)
     */
    public record Step(String kind, String target, String slug, String title, String reasonCode, String reason) { }

    public record Response(Basis basis, List<Step> reviews, List<Step> concepts, List<Step> terms,
                           String rulesVersion, Instant generatedAt, String notice) { }
}
