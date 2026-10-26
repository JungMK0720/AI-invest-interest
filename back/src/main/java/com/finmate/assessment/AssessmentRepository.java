package com.finmate.assessment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, String> {
    java.util.Optional<Assessment> findByUserIdAndSubmissionPeriod(String userId, String submissionPeriod);
    org.springframework.data.domain.Slice<Assessment> findByUserIdAndStatusOrderByCreatedAtDescIdDesc(
            String userId, AssessmentStatus status, org.springframework.data.domain.Pageable pageable);
    long countByStatus(AssessmentStatus status);
}
