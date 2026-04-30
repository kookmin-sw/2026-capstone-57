package com.ilgiyebo.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증된 사용자만 접근 가능한 API에 붙이는 어노테이션.
 * 유효한 JWT 토큰이 없으면 401 응답을 반환한다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MemberGuard {
}
