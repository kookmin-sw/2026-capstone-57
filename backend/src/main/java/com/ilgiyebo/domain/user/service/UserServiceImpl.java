package com.ilgiyebo.service;

import com.ilgiyebo.domain.UserEntity;
import com.ilgiyebo.domain.user.exception.UserException;
import com.ilgiyebo.dto.ParsedEmailInfo;
import com.ilgiyebo.dto.ProfileSetup;
import com.ilgiyebo.dto.UserProfileDto;
import com.ilgiyebo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final EmailParsingService emailParsingService;

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

    private void applyProfile(UserEntity user, ProfileSetup profile) {
        user.setNickname(profile.nickname());
        user.setName(profile.name());
        user.setMajor(profile.major());
        user.setBirthDate(profile.birthDate());
        user.setGender(profile.gender());
        user.setHobbies(profile.hobbies());
        user.setInterests(profile.interests());
        user.setPersonalityTypes(profile.personalityTypes());
        user.setIdealTypes(profile.idealTypes());
    }
}
