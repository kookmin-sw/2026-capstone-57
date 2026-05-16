package com.ilgiyebo.domain.user.service;

import com.ilgiyebo.domain.user.dto.ProfileSetup;
import com.ilgiyebo.domain.user.dto.UserProfileDto;

import java.util.UUID;

public interface UserService {

    /** 프로필 조회 */
    UserProfileDto getProfile(UUID userId);

    /** 프로필 수정 */
    UserProfileDto updateProfile(UUID userId, ProfileSetup updates);
}
