package com.ilgiyebo.service;

import com.ilgiyebo.dto.ProfileSetup;
import com.ilgiyebo.dto.UserProfileDto;

import java.util.UUID;

public interface UserService {

    /** 프로필 설정 (이름, 전공, 취미, 관심사, 성격 유형, 이상형, 생년월일, 성별) */
    UserProfileDto setupProfile(UUID userId, ProfileSetup profile);

    /** 프로필 조회 */
    UserProfileDto getProfile(UUID userId);

    /** 프로필 수정 */
    UserProfileDto updateProfile(UUID userId, ProfileSetup updates);
}
