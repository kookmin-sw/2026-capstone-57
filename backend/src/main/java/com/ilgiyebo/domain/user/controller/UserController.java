package com.ilgiyebo.controller;

import com.ilgiyebo.common.annotation.CurrentMember;
import com.ilgiyebo.common.annotation.MemberGuard;
import com.ilgiyebo.dto.ProfileSetup;
import com.ilgiyebo.dto.UserProfileDto;
import com.ilgiyebo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @MemberGuard
    @PostMapping("/profile")
    public ResponseEntity<UserProfileDto> setupProfile(
            @CurrentMember UUID userId,
            @Valid @RequestBody ProfileSetup profile) {
        return ResponseEntity.ok(userService.setupProfile(userId, profile));
    }

    @MemberGuard
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile(@CurrentMember UUID userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @MemberGuard
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @CurrentMember UUID userId,
            @Valid @RequestBody ProfileSetup updates) {
        return ResponseEntity.ok(userService.updateProfile(userId, updates));
    }
}
