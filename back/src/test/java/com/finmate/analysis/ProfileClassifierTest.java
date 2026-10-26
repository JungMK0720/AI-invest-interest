package com.finmate.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileClassifierTest {

    private final ProfileClassifier classifier = new ProfileClassifier();

    private ScoredAnswer answer(int weight, int score) {
        return new ScoredAnswer("문항", weight, "선택지", "KEY", score);
    }

    @Test
    @DisplayName("모든 문항에서 최고 점수를 고르면 공격투자형으로 분류된다")
    void allMaxScoreBecomesAggressive() {
        ProfileClassifier.ClassificationResult result = classifier.classify(List.of(
                answer(20, 100), answer(15, 100), answer(20, 100), answer(20, 100), answer(25, 100)));

        assertThat(result.totalScore()).isEqualTo(100);
        assertThat(result.profileType()).isEqualTo(InvestorProfileType.AGGRESSIVE);
    }

    @Test
    @DisplayName("모든 문항에서 최저 점수를 고르면 안정형으로 분류된다")
    void allMinScoreBecomesStable() {
        ProfileClassifier.ClassificationResult result = classifier.classify(List.of(
                answer(20, 0), answer(15, 0), answer(20, 0), answer(20, 0), answer(25, 0)));

        assertThat(result.totalScore()).isZero();
        assertThat(result.profileType()).isEqualTo(InvestorProfileType.STABLE);
    }

    @Test
    @DisplayName("배점 비중이 큰 문항의 답변이 총점에 더 크게 반영된다")
    void weightAffectsTotalScore() {
        // 배점 25 문항만 만점, 나머지는 0점 → 25점
        ProfileClassifier.ClassificationResult result = classifier.classify(List.of(
                answer(20, 0), answer(15, 0), answer(20, 0), answer(20, 0), answer(25, 100)));

        assertThat(result.totalScore()).isEqualTo(25);
    }

    @Test
    @DisplayName("성향 경계 점수는 상위 등급에 포함되지 않는다")
    void boundaryScoreBelongsToLowerType() {
        assertThat(InvestorProfileType.fromScore(25)).isEqualTo(InvestorProfileType.STABLE);
        assertThat(InvestorProfileType.fromScore(26)).isEqualTo(InvestorProfileType.CONSERVATIVE);
        assertThat(InvestorProfileType.fromScore(45)).isEqualTo(InvestorProfileType.CONSERVATIVE);
        assertThat(InvestorProfileType.fromScore(46)).isEqualTo(InvestorProfileType.NEUTRAL);
        assertThat(InvestorProfileType.fromScore(65)).isEqualTo(InvestorProfileType.NEUTRAL);
        assertThat(InvestorProfileType.fromScore(66)).isEqualTo(InvestorProfileType.ACTIVE);
        assertThat(InvestorProfileType.fromScore(80)).isEqualTo(InvestorProfileType.ACTIVE);
        assertThat(InvestorProfileType.fromScore(81)).isEqualTo(InvestorProfileType.AGGRESSIVE);
    }

    @Test
    @DisplayName("같은 답변은 항상 같은 등급을 만든다")
    void classificationIsDeterministic() {
        List<ScoredAnswer> answers = List.of(
                answer(20, 75), answer(15, 66), answer(20, 100), answer(20, 50), answer(25, 75));

        ProfileClassifier.ClassificationResult first = classifier.classify(answers);
        ProfileClassifier.ClassificationResult second = classifier.classify(answers);

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("답변이 없으면 성향을 판정하지 않는다")
    void emptyAnswersRejected() {
        assertThatThrownBy(() -> classifier.classify(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
