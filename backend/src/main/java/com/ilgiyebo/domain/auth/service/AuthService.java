package com.ilgiyebo.service;

import com.ilgiyebo.dto.AuthTokenResponse;
import com.ilgiyebo.dto.VerificationConfirmResponse;
import com.ilgiyebo.dto.VerificationResponse;

public interface AuthService {

    /** 대학 이메일로 인증 코드 발송 */
    VerificationResponse sendVerification(String email);

    /** 인증 코드 확인 (코드 일치 여부만 검증) */
    VerificationConfirmResponse confirmVerification(String verificationId, String code);

    /** 회원가입 (인증 완료 후 사용자 정보 입력) */
    AuthTokenResponse signup(String verificationId, String password,
                             String nickname, String major, String studentId);

    /** 로그인 */
    AuthTokenResponse login(String email, String password);
}
