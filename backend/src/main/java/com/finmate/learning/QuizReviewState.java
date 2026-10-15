package com.finmate.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** 사용자·문항별 최신 상태. 이전 시도는 quiz_attempts에 불변 이력으로 남긴다. */
@Entity
@Table(name = "quiz_review_state",
        indexes = @Index(name = "idx_quiz_review_user_due", columnList = "user_id,due_at"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "question_id"}))
public class QuizReviewState {

    private static final int[] INTERVAL_DAYS = {1, 3, 7, 14, 30};

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "concept_slug", nullable = false)
    private String slug;

    @Column(name = "concept_title", nullable = false)
    private String title;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(name = "content_version", nullable = false)
    private String contentVersion;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(name = "selected_choice_id")
    private String selectedChoiceId;

    @Column(name = "correct_choice_id", nullable = false)
    private String correctChoiceId;

    @Column(name = "selected_rationale", columnDefinition = "text")
    private String selectedRationale;

    @Column(name = "correct_rationale", nullable = false, columnDefinition = "text")
    private String correctRationale;

    @Column(nullable = false, columnDefinition = "text")
    private String takeaway;

    @Column(nullable = false)
    private boolean incorrect;

    @Column(nullable = false)
    private int streak;

    @Column(name = "last_attempted_at", nullable = false)
    private Instant lastAttemptedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    protected QuizReviewState() { }

    public QuizReviewState(String id, String userId, String slug, String title, String questionId) {
        this.id = id;
        this.userId = userId;
        this.slug = slug;
        this.title = title;
        this.questionId = questionId;
    }

    public void apply(QuizAttemptAnswerRecord answer, String contentVersion, Instant attemptedAt) {
        if (this.contentVersion != null && !this.contentVersion.equals(contentVersion)) {
            this.streak = 0;
        }
        this.contentVersion = contentVersion;
        this.prompt = answer.getPrompt();
        this.selectedChoiceId = answer.getSelectedChoiceId();
        this.correctChoiceId = answer.getCorrectChoiceId();
        this.selectedRationale = answer.getSelectedRationale();
        this.correctRationale = answer.getCorrectRationale();
        this.takeaway = answer.getTakeaway();
        this.incorrect = !answer.isCorrect();
        this.streak = incorrect ? 0 : Math.min(streak + 1, INTERVAL_DAYS.length);
        this.lastAttemptedAt = attemptedAt;
        int interval = INTERVAL_DAYS[Math.max(0, streak - 1)];
        this.dueAt = attemptedAt.plus(interval, ChronoUnit.DAYS);
    }

    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getQuestionId() { return questionId; }
    public String getPrompt() { return prompt; }
    public String getSelectedChoiceId() { return selectedChoiceId; }
    public String getCorrectChoiceId() { return correctChoiceId; }
    public String getSelectedRationale() { return selectedRationale; }
    public String getCorrectRationale() { return correctRationale; }
    public String getTakeaway() { return takeaway; }
    public boolean isIncorrect() { return incorrect; }
    public int getStreak() { return streak; }
    public Instant getLastAttemptedAt() { return lastAttemptedAt; }
    public Instant getDueAt() { return dueAt; }
}
