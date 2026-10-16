package com.finmate.learning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.finmate.common.ApiException;
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
        LearningTree tree = catalog.current();
        LearningContent.Node node = require(tree, slug);
        List<LearningDtos.QuizQuestion> questions = node.quiz().stream()
                .map(item -> new LearningDtos.QuizQuestion(
                        item.id(),
                        item.prompt(),
                        item.difficulty() == null ? "BASIC" : item.difficulty(),
                        item.options().stream().map(LearningContent.Option::label).toList()))
                .toList();
        return new LearningDtos.QuizResponse(
                node.slug(), node.title(), tree.version(), questions, LearningDtos.EDUCATION_NOTICE);
    }

    /**
     * ID 기반 제출 계약. 화면 인덱스가 아니라 콘텐츠 버전과 고정 choice ID를 검증하므로
     * 선택지 순서 변경이나 다른 개념의 문항을 섞어 보내는 실수를 숨기지 않는다.
     * 버전 비교와 채점은 같은 트리 한 벌로 해, 그사이 게시가 끼어도 옛 문항을 새 버전으로 채점하지 않는다.
     */
    public LearningDtos.QuizAttemptResponse attempt(String slug, LearningDtos.QuizAttemptRequest request) {
        LearningTree tree = catalog.current();
        LearningContent.Node node = require(tree, slug);
        if (request == null || request.contentVersion() == null || request.contentVersion().isBlank()) {
            throw ApiException.badRequest("contentVersion은 필수입니다.");
        }
        if (!tree.version().equals(request.contentVersion())) {
            throw ApiException.conflict("콘텐츠 버전이 변경되었습니다. 문제를 다시 불러오세요.");
        }
        if (request.answers() == null || request.answers().isEmpty()) {
            throw ApiException.badRequest("최소 한 문항은 선택해야 합니다.");
        }
        Map<String, LearningDtos.QuizAttemptAnswer> submitted = new LinkedHashMap<>();
        for (LearningDtos.QuizAttemptAnswer answer : request.answers()) {
            if (answer == null || answer.questionId() == null || answer.questionId().isBlank()) {
                throw ApiException.badRequest("questionId가 없는 답변입니다.");
            }
            if (submitted.put(answer.questionId(), answer) != null) {
                throw ApiException.badRequest("같은 문항을 두 번 제출했습니다: " + answer.questionId());
            }
        }
        Map<String, LearningContent.QuizItem> questions = new LinkedHashMap<>();
        node.quiz().forEach(item -> questions.put(item.id(), item));
        for (LearningDtos.QuizAttemptAnswer answer : submitted.values()) {
            LearningContent.QuizItem item = questions.get(answer.questionId());
            if (item == null) {
                throw ApiException.badRequest("이 개념에 속하지 않는 문항입니다: " + answer.questionId());
            }
            if (answer.choiceId() != null && !choiceIds(item).contains(answer.choiceId())) {
                throw ApiException.badRequest("이 문항에 속하지 않는 선택지입니다: " + answer.choiceId());
            }
        }
        List<LearningDtos.QuizAttemptItem> items = new ArrayList<>();
        int correctCount = 0;
        int answered = 0;
        for (LearningContent.QuizItem item : node.quiz()) {
            LearningDtos.QuizAttemptAnswer answer = submitted.get(item.id());
            String selected = answer == null ? null : answer.choiceId();
            String correct = choiceId(item, correctIndexOf(item));
            boolean isCorrect = selected != null && selected.equals(correct);
            if (selected != null) answered++;
            if (isCorrect) correctCount++;
            List<LearningDtos.QuizAttemptOption> choices = new ArrayList<>();
            for (int i = 0; i < item.options().size(); i++) {
                LearningContent.Option option = item.options().get(i);
                choices.add(new LearningDtos.QuizAttemptOption(
                        choiceId(item, i), option.label(), option.correct(), option.rationale()));
            }
            items.add(new LearningDtos.QuizAttemptItem(item.id(), item.prompt(), selected, correct,
                    isCorrect, List.copyOf(choices), item.takeaway()));
        }
        int total = items.size();
        int score = total == 0 ? 0 : (int) Math.round(correctCount * 100.0 / total);
        return new LearningDtos.QuizAttemptResponse(node.slug(), total, answered, correctCount, score,
                tree.version(), List.copyOf(items), summary(node, correctCount, total), LearningDtos.EDUCATION_NOTICE);
    }

    private static List<String> choiceIds(LearningContent.QuizItem item) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < item.options().size(); i++) ids.add(choiceId(item, i));
        return ids;
    }

    private static String choiceId(LearningContent.QuizItem item, int index) {
        return item.id() + "-choice-" + index;
    }

    public LearningDtos.QuizGradeResponse grade(String slug, LearningDtos.QuizGradeRequest request) {
        LearningTree tree = catalog.current();
        LearningContent.Node node = require(tree, slug);
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
                node.slug(), total, correctCount, scorePercent, tree.version(), items, summary(node, correctCount, total));
    }

    private static LearningContent.Node require(LearningTree tree, String slug) {
        return tree.lookup(slug)
                .orElseThrow(() -> ApiException.notFound("학습 개념을 찾을 수 없습니다: " + slug));
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
        // 트리 검증(LearningTree)을 통과했다면 도달하지 않는다.
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
