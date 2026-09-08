package com.finmate.analysis;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 투자자 성향 판정 규칙 엔진.
 *
 * 성향 등급은 규제 대상 판단이므로 LLM이 아니라 결정적 규칙으로 산출한다.
 * 같은 답변은 언제나 같은 등급을 만들고, 산출 근거를 점수로 재현할 수 있어야 하기 때문이다.
 */
@Component
public class ProfileClassifier {

    public ClassificationResult classify(List<ScoredAnswer> answers) {
        if (answers.isEmpty()) {
            throw new IllegalArgumentException("성향 판정에 사용할 답변이 없습니다.");
        }
        int totalWeight = answers.stream().mapToInt(ScoredAnswer::weight).sum();
        if (totalWeight <= 0) {
            throw new IllegalStateException("문항 배점 합이 0입니다. 진단 문항 설정을 확인해야 합니다.");
        }
        int weightedSum = answers.stream()
                .mapToInt(answer -> answer.score() * answer.weight())
                .sum();
        int totalScore = Math.round((float) weightedSum / totalWeight);
        return new ClassificationResult(totalScore, InvestorProfileType.fromScore(totalScore));
    }

    public record ClassificationResult(int totalScore, InvestorProfileType profileType) {
    }
}
