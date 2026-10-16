package com.finmate.learning;

/**
 * 공개 개념 집합이나 본문·확인 문제가 바뀌었음을 알린다. {@link LearningCatalog}가 커밋 뒤에 받아 캐시를 버린다.
 *
 * @param slug   바뀐 개념. 시드 적재처럼 여러 건이 한 번에 바뀌면 null
 * @param reason SEEDED, PUBLISHED, UNPUBLISHED 중 하나
 */
public record LearningContentChangedEvent(String slug, String reason) {
}
