package com.ilgiyebo.common.aop;

import com.ilgiyebo.common.exception.BusinessException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MemberGuardAspect {

    @Before("@annotation(com.ilgiyebo.common.annotation.MemberGuard)")
    public void checkAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }
    }
}
