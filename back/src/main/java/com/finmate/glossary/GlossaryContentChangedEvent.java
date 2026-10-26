package com.finmate.glossary;

/**
 * 공개 용어 집합이나 본문이 바뀌었음을 알린다. 검색 인덱스처럼 공개 용어로 만든 캐시가 이 이벤트를 듣고 다시 만든다.
 *
 * @param slug   바뀐 용어. 시드 적재처럼 여러 건이 한 번에 바뀌면 null
 * @param reason SEEDED, PUBLISHED, UNPUBLISHED, CREATED 중 하나
 */
public record GlossaryContentChangedEvent(String slug, String reason) {
}
