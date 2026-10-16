package com.finmate.assessment;

import com.finmate.analysis.ProfileClassifier;
import com.finmate.analysis.ScoredAnswer;
import com.finmate.analysis.SuitabilityMatcher;
import com.finmate.analysis.ReasoningService;
import com.finmate.common.ApiException;
import com.finmate.diagnosis.Question;
import com.finmate.diagnosis.QuestionChoice;
import com.finmate.diagnosis.QuestionChoiceRepository;
import com.finmate.diagnosis.QuestionRepository;
import com.finmate.product.ProductCategoryRepository;
import com.finmate.user.User;
import com.finmate.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 진단의 트랜잭션 경계를 담당한다.
 *
 * 저장(prepare) → 외부 AI 호출 → 저장(complete) 로 나눈 이유는, AI 호출이 진행되는 동안
 * DB 커넥션을 붙잡고 있지 않도록 하기 위해서다. AI 응답이 느려도 커넥션 풀이 마르지 않는다.
 */
@Service
public class AssessmentStore {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionChoiceRepository questionChoiceRepository;
    private final ProductCategoryRepository categoryRepository;
    private final AssessmentRepository assessmentRepository;
    private final AnswerRepository answerRepository;
    private final InvestmentProfileRepository profileRepository;
    private final CategoryRecommendationRepository recommendationRepository;
    private final AssessmentPeriodPolicy periodPolicy;
    private final SnapshotCodec snapshotCodec;
    private final com.finmate.analysis.AnswerReviewPolicy reviewPolicy;
    private final ProfileClassifier classifier;
    private final SuitabilityMatcher suitabilityMatcher;

    public AssessmentStore(UserRepository userRepository,
                           QuestionRepository questionRepository,
                           QuestionChoiceRepository questionChoiceRepository,
                           ProductCategoryRepository categoryRepository,
                           AssessmentRepository assessmentRepository,
                           AnswerRepository answerRepository,
                           InvestmentProfileRepository profileRepository,
                           CategoryRecommendationRepository recommendationRepository,
                           ProfileClassifier classifier,
                           SuitabilityMatcher suitabilityMatcher,
                           AssessmentPeriodPolicy periodPolicy,
                           SnapshotCodec snapshotCodec,
                           com.finmate.analysis.AnswerReviewPolicy reviewPolicy) {
        this.periodPolicy = periodPolicy;
        this.snapshotCodec = snapshotCodec;
        this.reviewPolicy = reviewPolicy;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.questionChoiceRepository = questionChoiceRepository;
        this.categoryRepository = categoryRepository;
        this.assessmentRepository = assessmentRepository;
        this.answerRepository = answerRepository;
        this.profileRepository = profileRepository;
        this.recommendationRepository = recommendationRepository;
        this.classifier = classifier;
        this.suitabilityMatcher = suitabilityMatcher;
    }

    @Transactional
    public AssessmentDtos.PreparedAnalysis prepare(String userId, AssessmentDtos.SubmitRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("로그인이 필요합니다."));

        if (!findAvailability(userId).canSubmit()) throw monthlyLimit();
        List<Question> activeQuestions = questionRepository.findByActiveTrueOrderByOrderNoAsc();
        if (activeQuestions.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ANALYSIS_UNAVAILABLE",
                    "진단 문항이 준비되지 않았습니다. 잠시 후 다시 시도해 주세요.");
        }

        if (activeQuestions.stream().mapToInt(Question::getWeight).sum() != 100
                || activeQuestions.stream().anyMatch(q -> q.getWeight() <= 0 || q.getChoices().isEmpty()
                || q.getChoices().stream().anyMatch(c -> c.getScore() < 0 || c.getScore() > 100))) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ANALYSIS_UNAVAILABLE",
                    "진단 문항 설정을 확인 중입니다. 잠시 후 다시 시도해 주세요.");
        }

        Map<String, Question> questionsById = new LinkedHashMap<>();
        activeQuestions.forEach(question -> questionsById.put(question.getId(), question));

        Assessment draft = new Assessment(UUID.randomUUID().toString(), user, periodPolicy.now());
        draft.reservePeriod(periodPolicy.period());
        // 식별자를 직접 부여한 엔터티는 save가 merge로 처리되어 관리 상태의 사본을 돌려준다.
        // 이후 스냅샷 기록이 DB에 반영되도록 반드시 반환된 인스턴스를 쓴다.
        Assessment assessment = assessmentRepository.save(draft);

        Set<String> answeredQuestionIds = new HashSet<>();
        List<ScoredAnswer> scoredAnswers = new ArrayList<>();
        List<DecisionSnapshot.AnswerEvidence> evidence = new ArrayList<>();

        for (AssessmentDtos.AnswerItem item : request.answers()) {
            Question question = questionsById.get(item.questionId());
            if (question == null) {
                throw ApiException.badRequest("존재하지 않거나 사용하지 않는 문항입니다: " + item.questionId());
            }
            if (!answeredQuestionIds.add(item.questionId())) {
                throw ApiException.badRequest("같은 문항에 중복으로 답변했습니다: " + item.questionId());
            }
            QuestionChoice choice = questionChoiceRepository.findById(item.choiceId())
                    .orElseThrow(() -> ApiException.badRequest("존재하지 않는 선택지입니다: " + item.choiceId()));
            if (!choice.getQuestion().getId().equals(question.getId())) {
                throw ApiException.badRequest("문항과 선택지가 일치하지 않습니다: " + item.questionId());
            }

            answerRepository.save(new Answer(UUID.randomUUID().toString(), assessment, question, choice));
            scoredAnswers.add(new ScoredAnswer(
                    question.getContent(),
                    question.getWeight(),
                    choice.getLabel(),
                    choice.getAnalysisKey(),
                    choice.getScore()));
            evidence.add(new DecisionSnapshot.AnswerEvidence(question.getId(), choice.getId(),
                    scoredAnswers.get(scoredAnswers.size() - 1)));
        }

        if (!answeredQuestionIds.containsAll(questionsById.keySet())) {
            throw ApiException.badRequest("모든 진단 문항에 답변해야 합니다.");
        }

        ProfileClassifier.ClassificationResult classification = classifier.classify(scoredAnswers);
        List<SuitabilityMatcher.RankedCategory> ranked = suitabilityMatcher.rank(
                classification.profileType(), categoryRepository.findAllByOrderByRiskGradeAsc());

        if (ranked.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ANALYSIS_UNAVAILABLE",
                    "안내 가능한 상품 카테고리가 없습니다. 잠시 후 다시 시도해 주세요.");
        }

        assessment.captureDecision(snapshotCodec.encode(new DecisionSnapshot(
                DecisionSnapshot.RULE_VERSION, snapshotCodec.questionnaireVersion(activeQuestions),
                evidence, ranked.stream().map(r -> new AssessmentDtos.CategoryRecommendationResponse(
                        r.category().getId(), r.category().getName(), r.category().getRiskGrade(),
                        r.category().riskGrade().label(), r.rankNo(), r.reason())).toList(),
                reviewPolicy.review(scoredAnswers))));

        return new AssessmentDtos.PreparedAnalysis(
                assessment.getId(),
                classification.profileType(),
                classification.totalScore(),
                scoredAnswers,
                ranked);
    }

    @Transactional
    public AssessmentDtos.AssessmentResultResponse complete(AssessmentDtos.PreparedAnalysis prepared,
                                                           ReasoningService.Reasoning reasoning) {
        Assessment assessment = assessmentRepository.findById(prepared.assessmentId())
                .orElseThrow(() -> ApiException.notFound("진단을 찾을 수 없습니다."));

        Instant now = Instant.now();
        InvestmentProfile profile = new InvestmentProfile(
                UUID.randomUUID().toString(),
                assessment,
                prepared.profileType(),
                prepared.totalScore(),
                reasoning.text(),
                reasoning.source(),
                now);
        profileRepository.save(profile);

        for (SuitabilityMatcher.RankedCategory ranked : prepared.rankedCategories()) {
            recommendationRepository.save(new CategoryRecommendation(
                    UUID.randomUUID().toString(),
                    assessment,
                    ranked.category(),
                    ranked.rankNo(),
                    ranked.reason(),
                    now));
        }

        assessment.complete(now);
        assessmentRepository.save(assessment);
        return toResult(assessment, profile);
    }

    @Transactional(readOnly = true)
    public AssessmentDtos.AssessmentResultResponse findResult(String userId, String assessmentId) {
        Assessment assessment = loadOwned(userId, assessmentId);
        InvestmentProfile profile = profileRepository.findByAssessmentId(assessmentId).orElse(null);
        return toResult(assessment, profile);
    }

    @Transactional(readOnly = true)
    public List<AssessmentDtos.CategoryRecommendationResponse> findRecommendations(String userId, String assessmentId) {
        Assessment assessment = loadOwned(userId, assessmentId);
        if (assessment.getStatus() != AssessmentStatus.COMPLETED) return List.of();
        DecisionSnapshot snapshot = snapshotCodec.decode(assessment.getDecisionSnapshot());
        if (snapshot != null) return snapshot.categories();
        return recommendationRepository.findByAssessmentIdOrderByRankNoAsc(assessmentId).stream()
                .map(recommendation -> new AssessmentDtos.CategoryRecommendationResponse(
                        recommendation.getCategory().getId(),
                        recommendation.getCategory().getName(),
                        recommendation.getCategory().getRiskGrade(),
                        recommendation.getCategory().riskGrade().label(),
                        recommendation.getRankNo(),
                        recommendation.getReason()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentDtos.AvailabilityResponse findAvailability(String userId) {
        Assessment current = assessmentRepository.findByUserIdAndSubmissionPeriod(userId, periodPolicy.period()).orElse(null);
        return new AssessmentDtos.AvailabilityResponse(current == null, AssessmentPeriodPolicy.VERSION,
                current == null ? null : periodPolicy.nextAvailableAt(),
                current == null ? null : current.getId(), current == null ? null : current.getStatus().name());
    }

    public ApiException monthlyLimit() {
        return new ApiException(HttpStatus.CONFLICT, "ASSESSMENT_MONTHLY_LIMIT",
                "이번 달 진단을 이미 제출했습니다. 다음 달 1일 00:00(한국 시간)부터 다시 진단할 수 있습니다. 기존 결과와 상품 기초 학습을 이용해 주세요.");
    }

    @Transactional
    public void failAndRelease(String assessmentId) {
        assessmentRepository.findById(assessmentId).ifPresent(a -> {
            a.fail("COMPLETION_ERROR");
            a.releaseFailedPeriod();
        });
    }

    @Transactional(readOnly = true)
    public AssessmentDtos.HistoryResponse findHistory(String userId, int page) {
        if (page < 0 || page > 10000) throw ApiException.badRequest("페이지 범위가 올바르지 않습니다.");
        var results = assessmentRepository.findByUserIdAndStatusOrderByCreatedAtDescIdDesc(
                userId, AssessmentStatus.COMPLETED, org.springframework.data.domain.PageRequest.of(page, 20));
        return new AssessmentDtos.HistoryResponse(results.getContent().stream()
                .map(a -> toResult(a, profileRepository.findByAssessmentId(a.getId()).orElse(null))).toList(),
                page, results.hasNext());
    }

    private Assessment loadOwned(String userId, String assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> ApiException.notFound("진단을 찾을 수 없습니다."));
        if (!assessment.isOwnedBy(userId)) {
            throw ApiException.forbidden("본인의 진단 결과만 조회할 수 있습니다.");
        }
        return assessment;
    }

    private AssessmentDtos.EvidenceResponse toEvidence(Assessment assessment) {
        DecisionSnapshot snapshot = snapshotCodec.decode(assessment.getDecisionSnapshot());
        if (snapshot == null) return null;
        return new AssessmentDtos.EvidenceResponse(snapshot.ruleVersion(), snapshot.questionnaireVersion(),
                snapshot.answers().stream().map(a -> new AssessmentDtos.EvidenceAnswer(
                        a.questionId(), a.value().questionContent(), a.value().choiceLabel())).toList(),
                snapshot.reviewFlags());
    }

    private AssessmentDtos.AssessmentResultResponse toResult(Assessment assessment, InvestmentProfile profile) {
        AssessmentDtos.ProfileResponse profileResponse = profile == null ? null
                : new AssessmentDtos.ProfileResponse(
                        profile.getProfileType().name(),
                        profile.getProfileType().label(),
                        profile.getProfileType().grade(),
                        profile.getTotalScore(),
                        profile.getReasoning(),
                        profile.getReasoningSource().name());
        return new AssessmentDtos.AssessmentResultResponse(
                assessment.getId(),
                assessment.getStatus().name(),
                profileResponse,
                assessment.getCreatedAt(),
                assessment.getCompletedAt(),
                toEvidence(assessment));
    }
}
