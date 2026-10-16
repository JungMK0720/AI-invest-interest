package com.finmate.learning;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningQuizItemRepository extends JpaRepository<LearningQuizItem, String> {
    List<LearningQuizItem> findByConceptSlugOrderBySortOrderAsc(String conceptSlug);
}
