package com.finmate.learning;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import com.finmate.common.ApiException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class QuizGraderTest {

    private static LearningCatalog catalog;
    private static QuizGrader grader;

    @BeforeAll
    static void setUp() {
        catalog = new LearningCatalog(new ObjectMapper());
        catalog.load();
        grader = new QuizGrader(catalog);
    }

    @Test
    void 문제_조회_응답에는_정답_정보가_들어있지_않다() {
        LearningDtos.QuizResponse response = grader.questions("risk-return");
        assertThat(response.questions()).isNotEmpty();
        // options는 라벨 문자열만 담는다. 정답 인덱스나 해설이 새어 나가면 확인 문제가 무력해진다.
        assertThat(response.questions().get(0).options()).allSatisfy(label -> assertThat(label).isNotBlank());
        assertThat(response.toString()).doesNotContain("correct=true");
    }

    @Test
    void 정답을_고르면_만점이고_해설이_모두_내려온다() {
        LearningContent.Node node = catalog.node("risk-return");
        List<LearningDtos.QuizAnswer> answers = node.quiz().stream()
                .map(item -> new LearningDtos.QuizAnswer(item.id(), correctIndex(item)))
                .toList();

        LearningDtos.QuizGradeResponse graded = grader.grade("risk-return", new LearningDtos.QuizGradeRequest(answers));

        assertThat(graded.correctCount()).isEqualTo(graded.total());
        assertThat(graded.scorePercent()).isEqualTo(100);
        assertThat(graded.items()).allSatisfy(item -> {
            assertThat(item.correct()).isTrue();
            assertThat(item.options()).allSatisfy(option -> assertThat(option.rationale()).isNotBlank());
        });
    }

    @Test
    void 미응답과_요청누락은_오답으로_채점되고_예외를_던지지_않는다() {
        LearningDtos.QuizGradeResponse empty = grader.grade("risk-return", null);
        assertThat(empty.correctCount()).isZero();
        assertThat(empty.items()).allSatisfy(item -> assertThat(item.selectedIndex()).isEqualTo(-1));

        LearningDtos.QuizGradeResponse outOfRange = grader.grade("risk-return",
                new LearningDtos.QuizGradeRequest(List.of(new LearningDtos.QuizAnswer("risk-return-q1", 99))));
        assertThat(outOfRange.items().get(0).correct()).isFalse();
        assertThat(outOfRange.items().get(0).correctIndex()).isNotNegative();
    }

    @Test
    void ID기반_시도는_버전과_선택지_소속을_검증하고_선택지별_해설을_돌려준다() {
        LearningContent.Node node = catalog.node("risk-return");
        LearningContent.QuizItem first = node.quiz().get(0);
        int correct = correctIndex(first);
        LearningDtos.QuizAttemptResponse response = grader.attempt("risk-return",
                new LearningDtos.QuizAttemptRequest(catalog.version(), List.of(
                        new LearningDtos.QuizAttemptAnswer(first.id(), first.id() + "-choice-" + correct))));
        assertThat(response.answered()).isEqualTo(1);
        assertThat(response.items()).hasSize(node.quiz().size());
        assertThat(response.items().get(0).choices()).allSatisfy(choice -> assertThat(choice.rationale()).isNotBlank());
        assertThat(response.items().get(0).correctChoiceId()).isEqualTo(first.id() + "-choice-" + correct);
    }

    @Test
    void ID기반_시도는_오답문항과_다른_개념의_ID를_400으로_거부한다() {
        LearningContent.Node node = catalog.node("risk-return");
        String valid = node.quiz().get(0).id() + "-choice-0";
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> grader.attempt("risk-return",
                new LearningDtos.QuizAttemptRequest(catalog.version(), List.of(
                        new LearningDtos.QuizAttemptAnswer("unknown", valid)))))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void 정답_위치는_한쪽에_고정되지_않는다() {
        long[] counts = new long[3];
        for (LearningContent.Node node : catalog.nodes()) for (LearningContent.QuizItem item : node.quiz()) counts[correctIndex(item)]++;
        assertThat(counts[0]).isGreaterThan(10);
        assertThat(counts[1]).isGreaterThan(10);
        assertThat(counts[2]).isGreaterThan(10);
    }

    private static int correctIndex(LearningContent.QuizItem item) {
        List<LearningContent.Option> options = item.options();
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).correct()) {
                return i;
            }
        }
        throw new IllegalStateException(item.id());
    }
}
