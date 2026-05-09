package com.ilgiyebo.domain.auth.controller;

import com.ilgiyebo.domain.auth.dto.*;
import com.ilgiyebo.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "회원가입, 이메일 인증, 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "인증 코드 발송", description = "대학 이메일로 6자리 인증 코드를 발송한다")
    @PostMapping("/verify/send")
    public ResponseEntity<VerificationResponse> sendVerification(
            @Valid @RequestBody SendVerificationRequest request) {
        return ResponseEntity.ok(authService.sendVerification(request.email()));
    }

    @Operation(summary = "인증 코드 확인", description = "발송된 인증 코드가 일치하는지 확인한다")
    @PostMapping("/verify/confirm")
    public ResponseEntity<VerificationConfirmResponse> confirmVerification(
            @Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(authService.confirmVerification(
                request.verificationId(), request.code()));
    }

    @Operation(summary = "회원가입", description = "이메일 인증 완료 후 프로필 정보를 포함하여 회원가입한다")
    @PostMapping("/signup")
    public ResponseEntity<AuthTokenResponse> signup(
            @Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받는다")
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }
}
