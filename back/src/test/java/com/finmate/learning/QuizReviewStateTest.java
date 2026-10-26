package com.finmate.learning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class QuizReviewStateTest {

    @Test
    void correctStreakExpandsReviewIntervalAndWrongOrRevisionResetsIt() {
        QuizReviewState state = new QuizReviewState("state", "user", "stocks", "주식", "q1");
        QuizAttemptAnswerRecord correct = answer(true);
        QuizAttemptAnswerRecord wrong = answer(false);
        Instant first = Instant.parse("2026-10-15T00:00:00Z");

        state.apply(correct, "v1", first);
        assertThat(state.getStreak()).isEqualTo(1);
        assertThat(state.getDueAt()).isEqualTo(first.plusSeconds(86400));

        state.apply(correct, "v1", first.plusSeconds(86400));
        assertThat(state.getStreak()).isEqualTo(2);
        assertThat(state.getDueAt()).isEqualTo(first.plusSeconds(86400 * 4));

        state.apply(wrong, "v1", first.plusSeconds(86400 * 4));
        assertThat(state.getStreak()).isZero();
        assertThat(state.isIncorrect()).isTrue();

        state.apply(correct, "v1", first.plusSeconds(86400 * 5));
        assertThat(state.getStreak()).isEqualTo(1);
        state.apply(correct, "v2", first.plusSeconds(86400 * 6));
        assertThat(state.getStreak()).isEqualTo(1);
    }

    private static QuizAttemptAnswerRecord answer(boolean correct) {
        return new QuizAttemptAnswerRecord("answer", "attempt", "q1", "질문", "choice-a",
                "choice-b", correct, "선택 이유", "정답 이유", "다음 행동");
    }
}
