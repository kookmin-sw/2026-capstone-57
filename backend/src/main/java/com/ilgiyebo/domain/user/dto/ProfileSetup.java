package com.ilgiyebo.dto;

import java.util.List;

public record ProfileSetup(
    String nickname,
    List<String> hobbies,
    List<String> interests,
    String personalityType,
    IdealTypePreferences idealTypePreferences
) {}
