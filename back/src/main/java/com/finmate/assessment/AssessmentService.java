package com.finmate.assessment;

import com.finmate.analysis.ReasoningRequest;
import com.finmate.analysis.ReasoningService;
import com.finmate.analysis.SuitabilityMatcher;
import com.finmate.product.ProductCategory;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 진단 제출 흐름의 오케스트레이터.
 *
 * 규칙 엔진이 성향과 탐색 카테고리를 확정한 뒤에야 설명 생성기를 호출한다.
 * 이 메서드에는 @Transactional 을 두지 않는다. AI 호출 구간을 트랜잭션 밖으로 빼기 위함이다.
 */
@Service
public class AssessmentService {

    private final AssessmentStore store;
    private final ReasoningService reasoningService;

    public AssessmentService(AssessmentStore store, ReasoningService reasoningService) {
        this.store = store;
        this.reasoningService = reasoningService;
    }

    public AssessmentDtos.AssessmentResultResponse submit(String userId, AssessmentDtos.SubmitRequest request) {
        AssessmentDtos.PreparedAnalysis prepared;
        try {
            prepared = store.prepare(userId, request);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시 제출의 마지막 방어선은 DB의 사용자/월 유일성 제약이다.
            if (!store.findAvailability(userId).canSubmit()) throw store.monthlyLimit();
            throw e;
        }

        List<String> categoryNames = prepared.rankedCategories().stream()
                .map(SuitabilityMatcher.RankedCategory::category)
                .map(ProductCategory::getName)
                .toList();

        ReasoningService.Reasoning reasoning = reasoningService.generate(new ReasoningRequest(
                prepared.profileType(),
                prepared.totalScore(),
                prepared.scoredAnswers(),
                categoryNames));

        try {
            return store.complete(prepared, reasoning);
        } catch (RuntimeException e) {
            store.failAndRelease(prepared.assessmentId());
            throw e;
        }
    }

    public AssessmentDtos.AvailabilityResponse findAvailability(String userId) {
        return store.findAvailability(userId);
    }

    public AssessmentDtos.HistoryResponse findHistory(String userId, int page) {
        return store.findHistory(userId, page);
    }

    public AssessmentDtos.AssessmentResultResponse findResult(String userId, String assessmentId) {
        return store.findResult(userId, assessmentId);
    }

    public List<AssessmentDtos.CategoryRecommendationResponse> findRecommendations(String userId, String assessmentId) {
        return store.findRecommendations(userId, assessmentId);
    }
}
