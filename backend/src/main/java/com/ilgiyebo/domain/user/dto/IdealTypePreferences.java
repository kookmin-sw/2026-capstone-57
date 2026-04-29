package com.ilgiyebo.dto;

import java.util.List;

public record IdealTypePreferences(
    List<String> preferredHobbies,
    List<String> preferredPersonalityTypes,
    List<String> preferredInterests
) {}
