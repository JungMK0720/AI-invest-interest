package com.finmate.learning;

import com.finmate.common.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizProgressService {

    private final QuizAttemptRecordRepository attempts;
    private final QuizAttemptAnswerRecordRepository answers;
    private final QuizReviewStateRepository reviews;
    private final LearningCatalog catalog;

    public QuizProgressService(QuizAttemptRecordRepository attempts, QuizAttemptAnswerRecordRepository answers,
                               QuizReviewStateRepository reviews, LearningCatalog catalog) {
        this.attempts = attempts;
        this.answers = answers;
        this.reviews = reviews;
        this.catalog = catalog;
    }

    @Transactional
    public void record(String userId, LearningDtos.QuizAttemptResponse result) {
        Instant now = Instant.now();
        String attemptId = UUID.randomUUID().toString();
        attempts.save(new QuizAttemptRecord(attemptId, userId, result, now));
        String title = catalog.contains(result.slug()) ? catalog.node(result.slug()).title() : result.slug();
        for (LearningDtos.QuizAttemptItem item : result.items()) {
            QuizAttemptAnswerRecord answer = new QuizAttemptAnswerRecord(
                    UUID.randomUUID().toString(), attemptId, item);
            answers.save(answer);
            QuizReviewState state = reviews.findByUserIdAndQuestionId(userId, item.questionId())
                    .orElseGet(() -> new QuizReviewState(UUID.randomUUID().toString(), userId,
                            result.slug(), title, item.questionId()));
            state.apply(answer, result.contentVersion(), now);
            reviews.save(state);
        }
    }

    @Transactional(readOnly = true)
    public QuizProgressDtos.AttemptHistory history(String userId, int page) {
        if (page < 0 || page > 10_000) {
            throw ApiException.badRequest("page는 0 이상 10000 이하여야 합니다.");
        }
        Page<QuizAttemptRecord> found = attempts.findByUserIdOrderByAttemptedAtDesc(
                userId, PageRequest.of(page, 20));
        return new QuizProgressDtos.AttemptHistory(found.stream().map(QuizProgressService::summary).toList(),
                page, found.hasNext());
    }

    @Transactional(readOnly = true)
    public QuizProgressDtos.AttemptDetail detail(String userId, String attemptId) {
        QuizAttemptRecord found = attempts.findById(attemptId)
                .filter(attempt -> attempt.getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("퀴즈 시도 이력을 찾을 수 없습니다."));
        List<QuizProgressDtos.AttemptAnswer> responseAnswers = answers.findByAttemptIdOrderByQuestionId(attemptId)
                .stream().map(answer -> new QuizProgressDtos.AttemptAnswer(
                        answer.getQuestionId(), answer.getPrompt(), answer.getSelectedChoiceId(),
                        answer.getCorrectChoiceId(), answer.isCorrect(), answer.getSelectedRationale(),
                        answer.getCorrectRationale(), answer.getTakeaway())).toList();
        return new QuizProgressDtos.AttemptDetail(summary(found), responseAnswers);
    }

    @Transactional(readOnly = true)
    public QuizProgressDtos.WrongAnswerList wrongAnswers(String userId) {
        return new QuizProgressDtos.WrongAnswerList(reviews.findByUserIdAndIncorrectTrueOrderByDueAtAsc(userId)
                .stream().map(state -> new QuizProgressDtos.WrongAnswer(
                        state.getSlug(), state.getQuestionId(), state.getPrompt(), state.getSelectedChoiceId(),
                        state.getCorrectChoiceId(), state.getSelectedRationale(), state.getCorrectRationale(),
                        state.getTakeaway(), state.getLastAttemptedAt(), state.getDueAt(), state.getStreak()))
                .toList());
    }

    @Transactional(readOnly = true)
    public QuizProgressDtos.ReviewSchedule reviewSchedule(String userId) {
        Instant now = Instant.now();
        Map<String, ScheduleAccumulator> byConcept = new LinkedHashMap<>();
        for (QuizReviewState state : reviews.findByUserIdOrderByDueAtAsc(userId)) {
            ScheduleAccumulator summary = byConcept.computeIfAbsent(state.getSlug(),
                    slug -> new ScheduleAccumulator(slug, state.getTitle(), state.getDueAt()));
            summary.questionCount++;
            if (state.isIncorrect()) {
                summary.incorrectCount++;
            }
            if (!state.getDueAt().isAfter(now)) {
                summary.dueNow = true;
            }
        }
        List<QuizProgressDtos.ReviewScheduleItem> items = new ArrayList<>();
        byConcept.values().forEach(item -> items.add(new QuizProgressDtos.ReviewScheduleItem(
                item.slug, item.title, item.dueAt, item.dueNow, item.questionCount, item.incorrectCount)));
        return new QuizProgressDtos.ReviewSchedule(List.copyOf(items));
    }

    private static QuizProgressDtos.AttemptSummary summary(QuizAttemptRecord attempt) {
        return new QuizProgressDtos.AttemptSummary(attempt.getId(), attempt.getSlug(), attempt.getAttemptedAt(),
                attempt.getContentVersion(), attempt.getTotal(), attempt.getAnswered(),
                attempt.getCorrectCount(), attempt.getScorePercent());
    }

    private static final class ScheduleAccumulator {
        private final String slug;
        private final String title;
        private final Instant dueAt;
        private boolean dueNow;
        private int questionCount;
        private int incorrectCount;

        private ScheduleAccumulator(String slug, String title, Instant dueAt) {
            this.slug = slug;
            this.title = title;
            this.dueAt = dueAt;
        }
    }
}
