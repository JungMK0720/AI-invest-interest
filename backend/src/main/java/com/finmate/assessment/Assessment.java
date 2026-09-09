package com.finmate.assessment;

import com.finmate.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "assessments", uniqueConstraints = @jakarta.persistence.UniqueConstraint(
        name = "uq_assessment_user_period", columnNames = {"user_id", "submission_period"}))
public class Assessment {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentStatus status;

    /** 사용자에게 직접 노출하지 않는 내부 실패 사유. */
    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "decision_snapshot", columnDefinition = "text")
    private String decisionSnapshot;

    public String getDecisionSnapshot() { return decisionSnapshot; }
    public void captureDecision(String snapshot) {
        if (decisionSnapshot != null) throw new IllegalStateException("진단 근거는 변경할 수 없습니다.");
        this.decisionSnapshot = snapshot;
    }

    @Column(name = "submission_period", length = 7)
    private String submissionPeriod;

    public void reservePeriod(String period) { this.submissionPeriod = period; }
    public void releaseFailedPeriod() { this.submissionPeriod = null; }

    protected Assessment() {
    }

    public Assessment(String id, User user, Instant createdAt) {
        this.id = id;
        this.user = user;
        this.status = AssessmentStatus.PROCESSING;
        this.createdAt = createdAt;
    }

    public void complete(Instant completedAt) {
        this.status = AssessmentStatus.COMPLETED;
        this.completedAt = completedAt;
        this.failureReason = null;
    }

    public void fail(String reason) {
        this.status = AssessmentStatus.FAILED;
        this.failureReason = reason;
    }

    public boolean isOwnedBy(String userId) {
        return user.getId().equals(userId);
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public AssessmentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
