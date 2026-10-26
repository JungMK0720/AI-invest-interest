package com.finmate.analysis;

import java.util.List;

/** 근거 문장 생성기에 넘기는 입력. 규칙 엔진이 확정한 결과와 사용자가 실제 고른 답변만 담는다. */
public record ReasoningRequest(
        InvestorProfileType profileType,
        int totalScore,
        List<ScoredAnswer> answers,
        List<String> recommendedCategoryNames
) {
}
