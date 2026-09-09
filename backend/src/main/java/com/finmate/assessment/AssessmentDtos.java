package com.finmate.assessment;

import com.finmate.analysis.ScoredAnswer;
import com.finmate.analysis.SuitabilityMatcher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

public final class AssessmentDtos {

    private AssessmentDtos() {
    }

    /** 사용자 식별자는 인증 컨텍스트에서 얻으므로 요청 본문에 userId 를 두지 않는다. */
    public record SubmitRequest(@NotEmpty @Valid List<AnswerItem> answers) {
    }

    public record AnswerItem(@NotBlank String questionId, @NotBlank String choiceId) {
    }

    public record ProfileResponse(
            String profileType,
            String profileLabel,
            int profileGrade,
            int totalScore,
            String reasoning,
            String reasoningSource) {
    }

    public record AssessmentResultResponse(
            String id,
            String status,
            ProfileResponse profile,
            Instant createdAt,
            Instant completedAt,
            EvidenceResponse evidence) {
    }

    public record CategoryRecommendationResponse(
            String categoryId,
            String categoryName,
            int riskGrade,
            String riskGradeLabel,
            int rank,
            String reason) {
    }

    public record AvailabilityResponse(boolean canSubmit, String policyVersion,
            Instant nextAvailableAt, String currentAssessmentId, String currentStatus) {}

    public record HistoryResponse(List<AssessmentResultResponse> items, int page, boolean hasNext) {}

    public record EvidenceAnswer(String questionId, String question, String answer) {}
    public record EvidenceResponse(String ruleVersion, String questionnaireVersion,
            List<EvidenceAnswer> answers, List<DecisionSnapshot.ReviewFlag> reviewFlags) {}

    /** 트랜잭션 밖에서 AI 호출을 하기 위해 1단계 결과를 담아 넘기는 중간 객체. */
    public record PreparedAnalysis(
            String assessmentId,
            com.finmate.analysis.InvestorProfileType profileType,
            int totalScore,
            List<ScoredAnswer> scoredAnswers,
            List<SuitabilityMatcher.RankedCategory> rankedCategories) {
    }
}
