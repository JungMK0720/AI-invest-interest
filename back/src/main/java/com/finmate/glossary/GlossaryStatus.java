package com.finmate.glossary;

/**
 * 공개 API는 PUBLISHED 용어만 돌려준다. DRAFT는 관리자가 새로 만들었거나 공개를 내린 용어로, 편집자에게만 보인다.
 */
public enum GlossaryStatus {
    DRAFT,
    PUBLISHED
}
