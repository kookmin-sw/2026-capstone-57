package com.ilgiyebo.dto;

import java.util.List;

public record UserProfileDto(
    String id,
    String email,
    String nickname,
    List<String> hobbies,
    List<String> interests,
    String personalityType,
    IdealTypePreferences idealTypePreferences,
    int totalExp,
    int currentLevel
) {}
