package com.ilgiyebo.domain.auth.service;

import com.ilgiyebo.domain.auth.dto.AuthTokenResponse;
import com.ilgiyebo.domain.auth.dto.SignupRequest;
import com.ilgiyebo.domain.auth.dto.VerificationConfirmResponse;
import com.ilgiyebo.domain.auth.dto.VerificationResponse;

public interface AuthService {

    /** 대학 이메일로 인증 코드 발송 */
    VerificationResponse sendVerification(String email);

    /** 인증 코드 확인 (코드 일치 여부만 검증) */
    VerificationConfirmResponse confirmVerification(String verificationId, String code);

    /** 회원가입 (인증 완료 후 프로필 포함 사용자 생성) */
    AuthTokenResponse signup(SignupRequest request);

    /** 로그인 */
    AuthTokenResponse login(String email, String password);
}
