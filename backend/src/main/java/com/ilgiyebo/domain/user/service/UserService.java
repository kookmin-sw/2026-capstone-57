package com.ilgiyebo.service;

import com.ilgiyebo.dto.ProfileSetup;
import com.ilgiyebo.dto.UserProfileDto;

import java.util.UUID;

public interface UserService {

    /** 프로필 조회 */
    UserProfileDto getProfile(UUID userId);

    /** 프로필 수정 */
    UserProfileDto updateProfile(UUID userId, ProfileSetup updates);
}
