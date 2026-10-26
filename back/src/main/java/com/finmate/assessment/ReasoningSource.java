package com.finmate.assessment;

/** 분석 근거 문장의 출처. AI 호출이 실패해도 진단 자체는 완료되며 TEMPLATE 로 대체된다. */
public enum ReasoningSource {
    LLM,
    TEMPLATE
}
