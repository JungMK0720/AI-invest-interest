package com.finmate.learning;

/**
 * 학습 화면은 PUBLISHED 개념만 보여 준다. DRAFT는 관리자가 공개를 내린 개념으로, 편집자에게만 보인다.
 */
public enum LearningConceptStatus {
    DRAFT,
    PUBLISHED
}
