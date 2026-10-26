package com.finmate.learning;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptAnswerRecordRepository extends JpaRepository<QuizAttemptAnswerRecord, String> {
    List<QuizAttemptAnswerRecord> findByAttemptIdOrderByQuestionId(String attemptId);
}
