package com.ilgiyebo.domain.user.controller;

import com.ilgiyebo.domain.user.dto.ProfileSetup;
import com.ilgiyebo.domain.user.dto.ProfileOptionsResponse;
import com.ilgiyebo.domain.user.dto.UserProfileDto;
import com.ilgiyebo.domain.user.service.ProfileOptionService;
import com.ilgiyebo.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "사용자", description = "프로필 조회, 수정 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfileOptionService profileOptionService;

    @Operation(summary = "프로필 옵션 조회", description = "취미, 관심사, 이상형 각 10개를 랜덤으로, MBTI 16개 전체를 반환한다")
    @GetMapping("/profile/options")
    public ResponseEntity<ProfileOptionsResponse> getProfileOptions() {
        return ResponseEntity.ok(profileOptionService.getRandomOptions());
    }

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필을 조회한다")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @Operation(summary = "프로필 수정", description = "프로필 정보를 수정한다")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ProfileSetup updates) {
        return ResponseEntity.ok(userService.updateProfile(userId, updates));
    }
}
