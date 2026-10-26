package com.finmate.assessment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRecommendationRepository extends JpaRepository<CategoryRecommendation, String> {

    List<CategoryRecommendation> findByAssessmentIdOrderByRankNoAsc(String assessmentId);
}
