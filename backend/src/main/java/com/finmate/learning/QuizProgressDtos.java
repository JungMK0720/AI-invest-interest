package com.finmate.learning;

import java.time.Instant;
import java.util.List;

public final class QuizProgressDtos {

    private QuizProgressDtos() { }

    public record AttemptSummary(String id, String slug, Instant attemptedAt, String contentVersion,
                                 int total, int answered, int correctCount, int scorePercent) { }

    public record AttemptHistory(List<AttemptSummary> items, int page, boolean hasNext) { }

    public record AttemptAnswer(String questionId, String prompt, String selectedChoiceId,
                                String correctChoiceId, boolean correct, String selectedRationale,
                                String correctRationale, String takeaway) { }

    public record AttemptDetail(AttemptSummary attempt, List<AttemptAnswer> answers) { }

    /** target은 CONCEPT(개념 확인 문제) 또는 TERM(용어 퀴즈)이다. 화면이 어디로 돌려보낼지 정한다. */
    public record WrongAnswer(String slug, String target, String title, String questionId, String prompt,
                              String selectedChoiceId, String correctChoiceId, String selectedRationale,
                              String correctRationale, String takeaway, Instant lastAttemptedAt, Instant dueAt,
                              int streak) { }

    public record WrongAnswerList(List<WrongAnswer> items) { }

    public record ReviewScheduleItem(String slug, String target, String title, Instant dueAt, boolean dueNow,
                                     int questionCount, int incorrectCount) { }

    public record ReviewSchedule(List<ReviewScheduleItem> items) { }
}
