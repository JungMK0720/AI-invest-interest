package com.finmate.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "quiz_attempts", indexes = @Index(name = "idx_quiz_attempt_user_time", columnList = "user_id,attempted_at"))
public class QuizAttemptRecord {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "concept_slug", nullable = false)
    private String slug;

    @Column(name = "content_version", nullable = false)
    private String contentVersion;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private int answered;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "score_percent", nullable = false)
    private int scorePercent;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected QuizAttemptRecord() { }

    public QuizAttemptRecord(String id, String userId, LearningDtos.QuizAttemptResponse result, Instant attemptedAt) {
        this(id, userId, result.slug(), result.contentVersion(), result.total(), result.answered(),
                result.correctCount(), result.scorePercent(), attemptedAt);
    }

    public QuizAttemptRecord(String id, String userId, String slug, String contentVersion,
                             int total, int answered, int correctCount, int scorePercent, Instant attemptedAt) {
        this.id = id;
        this.userId = userId;
        this.slug = slug;
        this.contentVersion = contentVersion;
        this.total = total;
        this.answered = answered;
        this.correctCount = correctCount;
        this.scorePercent = scorePercent;
        this.attemptedAt = attemptedAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getSlug() { return slug; }
    public String getContentVersion() { return contentVersion; }
    public int getTotal() { return total; }
    public int getAnswered() { return answered; }
    public int getCorrectCount() { return correctCount; }
    public int getScorePercent() { return scorePercent; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
