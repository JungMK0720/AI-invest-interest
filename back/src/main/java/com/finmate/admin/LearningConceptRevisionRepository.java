package com.finmate.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningConceptRevisionRepository extends JpaRepository<LearningConceptRevision, String> {
    List<LearningConceptRevision> findBySlugOrderByCreatedAtDesc(String slug);
}
