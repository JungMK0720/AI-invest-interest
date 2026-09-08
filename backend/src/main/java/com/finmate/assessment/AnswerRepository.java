package com.finmate.assessment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, String> {

    List<Answer> findByAssessmentId(String assessmentId);
}
