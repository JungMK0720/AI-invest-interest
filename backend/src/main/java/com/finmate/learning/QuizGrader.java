package com.finmate.learning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 확인 문제 채점기. 정답과 해설은 서버에만 두고, 채점 결과로만 노출한다.
 *
 * <p>미응답(-1)과 범위를 벗어난 인덱스는 오답으로 처리하되 예외를 던지지 않는다.
 * 학습 화면에서 한 문항을 건너뛴 것이 요청 전체의 실패가 되면 안 되기 때문이다.
 */
@Service
public class QuizGrader {

    private final LearningCatalog catalog;

    public QuizGrader(LearningCatalog catalog) {
        this.catalog = catalog;
    }

    public LearningDtos.QuizResponse questions(String slug) {
        LearningContent.Node node = catalog.node(slug);
        List<LearningDtos.QuizQuestion> questions = node.quiz().stream()
                .map(item -> new LearningDtos.QuizQuestion(
                        item.id(),
                        item.prompt(),
                        item.difficulty() == null ? "BASIC" : item.difficulty(),
                        item.options().stream().map(LearningContent.Option::label).toList()))
                .toList();
        return new LearningDtos.QuizResponse(
                node.slug(), node.title(), catalog.version(), questions, LearningDtos.EDUCATION_NOTICE);
    }

    public LearningDtos.QuizGradeResponse grade(String slug, LearningDtos.QuizGradeRequest request) {
        LearningContent.Node node = catalog.node(slug);
        Map<String, Integer> selectedByQuestion = new LinkedHashMap<>();
        if (request != null && request.answers() != null) {
            for (LearningDtos.QuizAnswer answer : request.answers()) {
                if (answer != null && answer.questionId() != null) {
                    selectedByQuestion.put(answer.questionId(), answer.selectedIndex());
                }
            }
        }

        List<LearningDtos.QuizGradedItem> items = new ArrayList<>();
        int correctCount = 0;
        for (LearningContent.QuizItem item : node.quiz()) {
            int selected = selectedByQuestion.getOrDefault(item.id(), -1);
            int correctIndex = correctIndexOf(item);
            boolean correct = selected == correctIndex;
            if (correct) {
                correctCount++;
            }
            items.add(new LearningDtos.QuizGradedItem(
                    item.id(), item.prompt(), selected, correctIndex, correct, feedback(item), item.takeaway()));
        }

        int total = items.size();
        int scorePercent = total == 0 ? 0 : (int) Math.round(correctCount * 100.0 / total);
        return new LearningDtos.QuizGradeResponse(
                node.slug(), total, correctCount, scorePercent, catalog.version(), items, summary(node, correctCount, total));
    }

    private static List<LearningDtos.OptionFeedback> feedback(LearningContent.QuizItem item) {
        List<LearningDtos.OptionFeedback> options = new ArrayList<>();
        List<LearningContent.Option> source = item.options();
        for (int i = 0; i < source.size(); i++) {
            LearningContent.Option option = source.get(i);
            options.add(new LearningDtos.OptionFeedback(i, option.label(), option.correct(), option.rationale()));
        }
        return List.copyOf(options);
    }

    private static int correctIndexOf(LearningContent.QuizItem item) {
        List<LearningContent.Option> options = item.options();
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).correct()) {
                return i;
            }
        }
        // 로드 시점 검증을 통과했다면 도달하지 않는다.
        throw new IllegalStateException("정답이 없는 확인 문제입니다: " + item.id());
    }

    /** 점수 자체보다 다음 행동을 남긴다. 높은 점수를 투자 적합성 판단으로 오인하지 않게 한다. */
    private static String summary(LearningContent.Node node, int correctCount, int total) {
        if (total == 0) {
            return "이 개념에는 확인 문제가 아직 없습니다.";
        }
        if (correctCount == total) {
            return "‘" + node.title() + "’의 확인 포인트를 모두 짚었습니다. 상품설명서에서 같은 항목을 직접 찾아보세요.";
        }
        if (correctCount * 2 >= total) {
            return "핵심은 잡았습니다. 틀린 문항의 해설을 읽고 구조 그림을 한 번 더 확인하세요.";
        }
        return "이 개념은 다시 볼 필요가 있습니다. 설명과 확인 포인트를 먼저 읽고 모의투자로 확인해 보세요.";
    }
}
