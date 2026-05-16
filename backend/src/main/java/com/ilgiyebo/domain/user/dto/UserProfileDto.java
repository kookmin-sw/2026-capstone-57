package com.ilgiyebo.domain.user.dto;

import com.ilgiyebo.domain.user.entity.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record UserProfileDto(
    String id,
    String email,
    String nickname,
    String name,
    String major,
    LocalDate birthDate,
    Gender gender,
    List<ProfileOptionDto> hobbies,
    List<ProfileOptionDto> interests,
    ProfileOptionDto personalityType,
    List<ProfileOptionDto> idealTypes,
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
                toOptionList(user.getHobbies(), Hobby.class),
                toOptionList(user.getInterests(), Interest.class),
                user.getPersonalityType() != null
                        ? new ProfileOptionDto(user.getPersonalityType().name(), user.getPersonalityType().getLabel())
                        : null,
                toOptionList(user.getIdealTypes(), IdealType.class),
                user.getTotalExp(),
                user.getCurrentLevel()
        );
    }

    private static <E extends Enum<E>> List<ProfileOptionDto> toOptionList(List<String> codes, Class<E> enumClass) {
        if (codes == null) return Collections.emptyList();
        return codes.stream()
                .map(code -> {
                    try {
                        E e = Enum.valueOf(enumClass, code);
                        String label = (String) e.getClass().getMethod("getLabel").invoke(e);
                        return new ProfileOptionDto(code, label);
                    } catch (Exception ex) {
                        return new ProfileOptionDto(code, code);
                    }
                })
                .toList();
    }
}
