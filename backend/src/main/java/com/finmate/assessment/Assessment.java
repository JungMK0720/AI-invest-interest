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
@Table(name = "assessments")
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
