package com.finmate.learning;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizReviewStateRepository extends JpaRepository<QuizReviewState, String> {
    Optional<QuizReviewState> findByUserIdAndQuestionId(String userId, String questionId);
    List<QuizReviewState> findByUserIdOrderByDueAtAsc(String userId);
    List<QuizReviewState> findByUserIdAndIncorrectTrueOrderByDueAtAsc(String userId);
}
