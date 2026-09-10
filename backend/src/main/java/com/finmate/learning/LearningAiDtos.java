package com.finmate.learning;

/** 학습 개념 AI 설명 API의 입출력 계약. */
public final class LearningAiDtos {

    public static final String NOTICE =
            "AI 설명은 승인된 학습 콘텐츠를 바탕으로 한 교육용 안내입니다. "
                    + "특정 상품의 가입·매매를 권유하지 않으며, 실제 판단 전 공식 상품설명서를 확인하세요.";

    private LearningAiDtos() {
    }

    /** question이 비어 있으면 개념을 초보자 눈높이로 다시 설명한다. */
    public record ExplainRequest(String question) {
    }

    public record ExplainResponse(
            String slug,
            String title,
            String answer,
            String source,
            String sourceName,
            String sourceUrl,
            String contentVersion,
            String notice) {
    }
}
