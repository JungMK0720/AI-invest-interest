package com.finmate.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증 컨텍스트에서 사용자 식별자를 주입한다.
 * 요청 본문으로 userId 를 받지 않기 위한 장치다 (04-api-design.md).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
