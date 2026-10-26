package com.finmate.assessment;

import com.finmate.analysis.ScoredAnswer;
import com.finmate.analysis.SuitabilityMatcher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AssessmentDtos {

    private AssessmentDtos() {
    }

    /**
     * 사용자 식별자는 인증 컨텍스트에서 얻으므로 요청 본문에 userId 를 두지 않는다.
     *
     * @param narrative 선택 입력. 투자에 대한 생각을 자유롭게 쓴 문장. 모델 추정 결과만 저장하고 원문은 저장하지 않는다.
     */
    public record SubmitRequest(@NotEmpty @Valid List<AnswerItem> answers,
                                @Size(max = 1000) String narrative) {
        public SubmitRequest(List<AnswerItem> answers) {
            this(answers, null);
        }
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
            List<EvidenceAnswer> answers, List<DecisionSnapshot.ReviewFlag> reviewFlags,
            NarrativeResponse narrative) {}

    /** 자유 서술의 모델 추정. 진단 등급과 별개인 보조 신호이며 원문은 포함하지 않는다. */
    public record NarrativeResponse(String status, String profileType, String profileLabel, Integer grade,
            double confidence, boolean mismatch, List<DecisionSnapshot.NarrativeTag> tags, String modelVersion) {}

    /** 트랜잭션 밖에서 AI 호출을 하기 위해 1단계 결과를 담아 넘기는 중간 객체. */
    public record PreparedAnalysis(
            String assessmentId,
            com.finmate.analysis.InvestorProfileType profileType,
            int totalScore,
            List<ScoredAnswer> scoredAnswers,
            List<SuitabilityMatcher.RankedCategory> rankedCategories) {
    }
}
