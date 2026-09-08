package com.finmate.common;

/** OAS 공통 에러 스키마. 내부 예외 메시지나 프롬프트는 담지 않는다. */
public record ErrorResponse(String code, String message) {
}
