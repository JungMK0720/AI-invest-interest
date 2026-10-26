package com.finmate.analysis;

import com.finmate.assessment.DecisionSnapshot.ReviewFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 정보가 부족한 재무 능력을 점수로 추정하지 않고 확인할 답변만 표시한다. */
@Component
public class AnswerReviewPolicy {
    public List<ReviewFlag> review(List<ScoredAnswer> answers) {
        Set<String> keys = answers.stream().map(ScoredAnswer::analysisKey).collect(Collectors.toSet());
        List<ReviewFlag> flags = new ArrayList<>();
        if (keys.contains("HORIZON_UNDER_6M"))
            flags.add(new ReviewFlag("SHORT_HORIZON", "6개월 안에 필요한 자금이라고 답했습니다. 상품을 탐색할 때 환매 기간과 중도 해지 조건을 확인하세요."));
        if (keys.contains("ASSET_RATIO_OVER_40"))
            flags.add(new ReviewFlag("HIGH_CONCENTRATION", "투자 자금이 금융자산의 40%를 넘는다고 답했습니다. 비상자금과 생활비가 별도로 확보되어 있는지 점검하세요."));
        if (keys.contains("PURPOSE_PRINCIPAL_PROTECTION") && answers.stream()
                .anyMatch(a -> a.analysisKey().startsWith("LOSS_") && a.score() >= 75))
            flags.add(new ReviewFlag("CONFLICTING_PREFERENCES", "원금 보전을 우선하면서 큰 손실도 감내한다고 답했습니다. 두 답변이 현재 목표를 함께 반영하는지 확인하세요."));
        return List.copyOf(flags);
    }
}
