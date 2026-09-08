package com.finmate.assessment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentProfileRepository extends JpaRepository<InvestmentProfile, String> {

    Optional<InvestmentProfile> findByAssessmentId(String assessmentId);
}
