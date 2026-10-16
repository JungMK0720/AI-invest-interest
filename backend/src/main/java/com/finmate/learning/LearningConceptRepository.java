package com.finmate.learning;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LearningConceptRepository extends JpaRepository<LearningConcept, String> {
    List<LearningConcept> findByStatus(LearningConceptStatus status);

    long countByStatus(LearningConceptStatus status);

    /**
     * 게시·공개 내리기는 트리 전체를 다시 검증하므로 개념 행 전체에 쓰기 잠금을 건다.
     * 항상 slug 순서로 잠가 두 관리자의 동시 게시가 교착 없이 차례로 처리된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LearningConcept c order by c.slug")
    List<LearningConcept> lockAll();
}
