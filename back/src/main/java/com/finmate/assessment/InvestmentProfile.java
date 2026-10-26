package com.finmate.assessment;

import com.finmate.analysis.InvestorProfileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 진단 1건에 대한 투자 성향 분석 결과.
 * profileType 과 totalScore 는 규칙 엔진이 결정하고, reasoning 만 AI가 생성한다.
 * reasoningSource 는 AI 생성문인지 폴백 템플릿인지를 구분한다 (ADR-008).
 */
@Entity
@Table(name = "investment_profiles")
public class InvestmentProfile {

    @Id
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false, unique = true)
    private Assessment assessment;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_type", nullable = false)
    private InvestorProfileType profileType;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(nullable = false, length = 2000)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(name = "reasoning_source", nullable = false)
    private ReasoningSource reasoningSource;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InvestmentProfile() {
    }

    public InvestmentProfile(String id, Assessment assessment, InvestorProfileType profileType,
                             int totalScore, String reasoning, ReasoningSource reasoningSource,
                             Instant createdAt) {
        this.id = id;
        this.assessment = assessment;
        this.profileType = profileType;
        this.totalScore = totalScore;
        this.reasoning = reasoning;
        this.reasoningSource = reasoningSource;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public InvestorProfileType getProfileType() {
        return profileType;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public String getReasoning() {
        return reasoning;
    }

    public ReasoningSource getReasoningSource() {
        return reasoningSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
