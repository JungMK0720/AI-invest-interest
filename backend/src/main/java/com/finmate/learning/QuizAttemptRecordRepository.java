package com.finmate.learning;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRecordRepository extends JpaRepository<QuizAttemptRecord, String> {
    Page<QuizAttemptRecord> findByUserIdOrderByAttemptedAtDesc(String userId, Pageable pageable);
}
