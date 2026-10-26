package com.finmate.glossary;

import com.finmate.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 공개 용어의 정의로 만드는 4지선다 퀴즈. 서버에 상태를 두지 않고 seed와 콘텐츠 버전으로 같은 문제를 다시 만든다.
 * 문항 응답에는 정답이 들어가지 않고, 채점 때만 정답·해설을 돌려준다.
 */
@Service
public class GlossaryQuizService {
    private final GlossaryService glossary;
    private final GlossaryCategoryRepository categories;

    public GlossaryQuizService(GlossaryService glossary, GlossaryCategoryRepository categories) {
        this.glossary = glossary;
        this.categories = categories;
    }

    public GlossaryDtos.QuizResponse questions(String category, Integer requestedCount, Long seed) {
        if (category != null && !category.isBlank() && !categories.existsById(category)) {
            throw ApiException.badRequest("존재하지 않는 용어 분류입니다: " + category);
        }
        int count = requestedCount == null ? 5 : requestedCount;
        if (count < 1 || count > 20) throw ApiException.badRequest("문항 수는 1~20개여야 합니다.");
        List<GlossaryTerm> published = glossary.listPublished();
        if (published.size() < 4) throw ApiException.conflict("퀴즈를 만들기에 공개된 용어가 부족합니다.");
        String version = glossary.contentVersion();
        List<GlossaryTerm> candidates = new ArrayList<>(published.stream()
                .filter(term -> category == null || category.isBlank() || category.equals(term.getCategoryId()))
                .toList());
        Collections.shuffle(candidates, new Random(seed == null ? 0 : seed));
        List<GlossaryDtos.QuizQuestion> questions = candidates.stream().limit(count)
                .map(term -> question(term, published, version))
                .toList();
        return new GlossaryDtos.QuizResponse(version, questions, GlossaryDtos.EDUCATION_NOTICE);
    }

    public GlossaryDtos.QuizGradeResponse grade(GlossaryDtos.QuizGradeRequest request) {
        if (request == null || request.contentVersion() == null || request.contentVersion().isBlank()) {
            throw ApiException.badRequest("contentVersion은 필수입니다.");
        }
        String version = glossary.contentVersion();
        if (!version.equals(request.contentVersion())) {
            throw ApiException.conflict("용어 콘텐츠 버전이 변경되었습니다. 문제를 다시 불러오세요.");
        }
        if (request.answers() == null || request.answers().isEmpty()) {
            throw ApiException.badRequest("최소 한 문항은 제출해야 합니다.");
        }
        List<GlossaryTerm> published = glossary.listPublished();
        Map<String, GlossaryTerm> byQuestionId = new HashMap<>();
        for (GlossaryTerm term : published) byQuestionId.put(questionId(term, version), term);
        Set<String> submitted = new HashSet<>();
        List<GlossaryDtos.QuizGradeItem> items = new ArrayList<>();
        int answered = 0;
        int correctCount = 0;
        for (GlossaryDtos.QuizAnswer answer : request.answers()) {
            if (answer == null || answer.questionId() == null || answer.questionId().isBlank()) {
                throw ApiException.badRequest("questionId가 없는 답변입니다.");
            }
            if (!submitted.add(answer.questionId())) {
                throw ApiException.badRequest("같은 문항을 두 번 제출했습니다: " + answer.questionId());
            }
            GlossaryTerm term = byQuestionId.get(answer.questionId());
            if (term == null) throw ApiException.badRequest("공개된 용어의 문항이 아닙니다.");
            GlossaryDtos.QuizQuestion question = question(term, published, version);
            String selected = answer.choiceId();
            if (selected != null && question.choices().stream().noneMatch(choice -> choice.id().equals(selected))) {
                throw ApiException.badRequest("이 문항에 속하지 않는 선택지입니다.");
            }
            String selectedText = selected == null ? null : question.choices().stream()
                    .filter(choice -> choice.id().equals(selected))
                    .findFirst().orElseThrow().text();
            String correctId = choiceId(term, term, version);
            boolean correct = correctId.equals(selected);
            if (selected != null) answered++;
            if (correct) correctCount++;
            items.add(new GlossaryDtos.QuizGradeItem(answer.questionId(), term.getSlug(), question.prompt(),
                    selected, selectedText, correctId, correct,
                    "정답은 ‘" + term.getTerm() + "’입니다. " + term.getDefinition()));
        }
        int score = (int) Math.round(correctCount * 100.0 / items.size());
        return new GlossaryDtos.QuizGradeResponse(items.size(), answered, correctCount, score,
                List.copyOf(items), version, GlossaryDtos.EDUCATION_NOTICE);
    }

    private GlossaryDtos.QuizQuestion question(GlossaryTerm term, List<GlossaryTerm> published, String version) {
        List<GlossaryTerm> pool = new ArrayList<>(published.stream()
                .filter(candidate -> candidate.getCategoryId().equals(term.getCategoryId()))
                .filter(candidate -> !candidate.getSlug().equals(term.getSlug()))
                .filter(candidate -> !candidate.getTerm().equals(term.getTerm()))
                .toList());
        Collections.shuffle(pool, new Random(term.getSlug().hashCode()));
        List<GlossaryTerm> choices = new ArrayList<>();
        choices.add(term);
        addDistinct(choices, pool);
        if (choices.size() < 4) addDistinct(choices, published);
        if (choices.size() < 4) throw ApiException.conflict("퀴즈 선택지를 만들기에 공개된 용어가 부족합니다.");
        Collections.shuffle(choices, new Random(term.getSlug().hashCode() ^ 0x5F3759DF));
        return new GlossaryDtos.QuizQuestion(questionId(term, version),
                "다음 설명에 해당하는 용어는 무엇인가요? " + term.getDefinition(),
                choices.stream().map(candidate -> new GlossaryDtos.QuizChoice(
                        choiceId(term, candidate, version), candidate.getTerm())).toList());
    }

    private static void addDistinct(List<GlossaryTerm> choices, List<GlossaryTerm> pool) {
        for (GlossaryTerm candidate : pool) {
            if (choices.size() >= 4) return;
            if (choices.stream().noneMatch(existing -> existing.getTerm().equals(candidate.getTerm()))) {
                choices.add(candidate);
            }
        }
    }

    private String questionId(GlossaryTerm term, String version) {
        return opaque("question:" + version + ":" + term.getSlug());
    }

    private String choiceId(GlossaryTerm question, GlossaryTerm choice, String version) {
        return opaque("choice:" + version + ":" + question.getSlug() + ":" + choice.getSlug());
    }

    private static String opaque(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
