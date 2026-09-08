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
                           SuitabilityMatcher suitabilityMatcher) {
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

        List<Question> activeQuestions = questionRepository.findByActiveTrueOrderByOrderNoAsc();
        if (activeQuestions.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ANALYSIS_UNAVAILABLE",
                    "진단 문항이 준비되지 않았습니다. 잠시 후 다시 시도해 주세요.");
        }

        Map<String, Question> questionsById = new LinkedHashMap<>();
        activeQuestions.forEach(question -> questionsById.put(question.getId(), question));

        Assessment assessment = new Assessment(UUID.randomUUID().toString(), user, Instant.now());
        assessmentRepository.save(assessment);

        Set<String> answeredQuestionIds = new HashSet<>();
        List<ScoredAnswer> scoredAnswers = new ArrayList<>();

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
        loadOwned(userId, assessmentId);
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

    private Assessment loadOwned(String userId, String assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> ApiException.notFound("진단을 찾을 수 없습니다."));
        if (!assessment.isOwnedBy(userId)) {
            throw ApiException.forbidden("본인의 진단 결과만 조회할 수 있습니다.");
        }
        return assessment;
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
                assessment.getCompletedAt());
    }
}
