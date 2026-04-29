package com.ilgiyebo.service;

public interface EmailService {
    /** 인증 코드 이메일 발송 */
    void sendVerificationEmail(String to, String code);
}
