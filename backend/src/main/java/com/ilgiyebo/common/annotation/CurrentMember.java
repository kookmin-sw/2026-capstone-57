package com.ilgiyebo.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드 파라미터에 붙여서 현재 인증된 사용자의 UUID를 주입받는다.
 * @MemberGuard와 함께 사용한다.
 *
 * 예시:
 * <pre>
 * {@code @MemberGuard}
 * {@code @GetMapping("/me")}
 * public ResponseEntity<UserProfile> getMyProfile(@CurrentMember UUID userId) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMember {
}
