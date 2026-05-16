package com.ilgiyebo.domain.exp.dto;

import java.util.UUID;

public record ExperienceInfoDto(
    UUID userId,
    int totalExp,
    int currentLevel,
    int expToNextLevel,
    int currentLevelExp
) {}
