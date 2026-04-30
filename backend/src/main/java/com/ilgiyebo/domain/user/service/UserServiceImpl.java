package com.ilgiyebo.service;

import com.ilgiyebo.domain.Gender;
import com.ilgiyebo.domain.SlotEntity;
import com.ilgiyebo.domain.SlotStatus;
import com.ilgiyebo.domain.UserEntity;
import com.ilgiyebo.domain.user.exception.UserException;
import com.ilgiyebo.dto.IdealTypePreferences;
import com.ilgiyebo.dto.ParsedEmailInfo;
import com.ilgiyebo.dto.ProfileSetup;
import com.ilgiyebo.dto.UserProfileDto;
import com.ilgiyebo.repository.SlotRepository;
import com.ilgiyebo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final SlotRepository slotRepository;
    private final EmailParsingService emailParsingService;

    @Override
    @Transactional
    public UserProfileDto setupProfile(UUID userId, ProfileSetup profile) {
        UserEntity user = findUserOrThrow(userId);

        if (isProfileComplete(user)) {
            throw UserException.PROFILE_ALREADY_SET.toException();
        }

        applyProfile(user, profile);
        user = userRepository.save(user);

        // 프로필 설정 완료 시 초기 슬롯 1개 자동 부여
        if (slotRepository.findByUserId(userId).isEmpty()) {
            SlotEntity slot = SlotEntity.builder()
                    .userId(userId)
                    .status(SlotStatus.EMPTY)
                    .build();
            slotRepository.save(slot);
            log.info("초기 슬롯 부여 완료: userId={}", userId);
        }

        return UserProfileDto.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        UserEntity user = findUserOrThrow(userId);
        return UserProfileDto.from(user);
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(UUID userId, ProfileSetup updates) {
        UserEntity user = findUserOrThrow(userId);
        applyProfile(user, updates);
        user = userRepository.save(user);
        return UserProfileDto.from(user);
    }

    /**
     * 이메일 파싱 서비스가 활성화된 경우, 이메일에서 이름/전공을 추출하여 반환한다.
     * 비활성화 상태이면 빈 Optional을 반환한다.
     */
    public Optional<ParsedEmailInfo> tryParseEmail(String email) {
        if (!emailParsingService.isEnabled()) {
            return Optional.empty();
        }
        return emailParsingService.parseEmail(email);
    }

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserException.USER_NOT_FOUND::toException);
    }

    private boolean isProfileComplete(UserEntity user) {
        return user.getName() != null
                && user.getBirthDate() != null
                && user.getGender() != null;
    }

    private void applyProfile(UserEntity user, ProfileSetup profile) {
        user.setNickname(profile.nickname());
        user.setName(profile.name());
        user.setMajor(profile.major());
        user.setBirthDate(profile.birthDate());
        user.setGender(profile.gender());
        user.setHobbies(profile.hobbies());
        user.setInterests(profile.interests());
        user.setPersonalityType(profile.personalityType());
        user.setIdealTypePreferences(toMap(profile.idealTypePreferences()));
    }

    private Map<String, Object> toMap(IdealTypePreferences prefs) {
        if (prefs == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("preferredHobbies", prefs.preferredHobbies());
        map.put("preferredPersonalityTypes", prefs.preferredPersonalityTypes());
        map.put("preferredInterests", prefs.preferredInterests());
        return map;
    }
}
