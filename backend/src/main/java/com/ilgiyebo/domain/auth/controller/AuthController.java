package com.ilgiyebo.controller;

import com.ilgiyebo.dto.*;
import com.ilgiyebo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/verify/send")
    public ResponseEntity<VerificationResponse> sendVerification(
            @Valid @RequestBody SendVerificationRequest request) {
        return ResponseEntity.ok(authService.sendVerification(request.email()));
    }

    @PostMapping("/verify/confirm")
    public ResponseEntity<VerificationConfirmResponse> confirmVerification(
            @Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(authService.confirmVerification(
                request.verificationId(), request.code()));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthTokenResponse> signup(
            @Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(
                request.verificationId(),
                request.password(),
                request.nickname(),
                request.major(),
                request.studentId()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }
}
