package com.ilgiyebo.domain.user.dto;

import com.ilgiyebo.domain.user.entity.Gender;
import com.ilgiyebo.domain.user.entity.UserEntity;

import java.time.LocalDate;
import java.util.List;

public record UserProfileDto(
    String id,
    String email,
    String nickname,
    String name,
    String major,
    LocalDate birthDate,
    Gender gender,
    List<String> hobbies,
    List<String> interests,
    List<String> personalityTypes,
    List<String> idealTypes,
    int totalExp,
    int currentLevel
) {
    public static UserProfileDto from(UserEntity user) {
        return new UserProfileDto(
                user.getId().toString(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                user.getMajor(),
                user.getBirthDate(),
                user.getGender(),
                user.getHobbies(),
                user.getInterests(),
                user.getPersonalityTypes(),
                user.getIdealTypes(),
                user.getTotalExp(),
                user.getCurrentLevel()
        );
    }
}
