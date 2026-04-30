package com.ilgiyebo.dto;

import com.ilgiyebo.domain.Gender;
import com.ilgiyebo.domain.UserEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    String personalityType,
    IdealTypePreferences idealTypePreferences,
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
                user.getPersonalityType(),
                toIdealTypePreferences(user.getIdealTypePreferences()),
                user.getTotalExp(),
                user.getCurrentLevel()
        );
    }

    private static IdealTypePreferences toIdealTypePreferences(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new IdealTypePreferences(
                toStringList(map.get("preferredHobbies")),
                toStringList(map.get("preferredPersonalityTypes")),
                toStringList(map.get("preferredInterests"))
        );
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return Collections.emptyList();
    }
}
