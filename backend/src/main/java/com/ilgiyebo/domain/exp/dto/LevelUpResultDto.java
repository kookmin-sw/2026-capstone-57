package com.ilgiyebo.domain.exp.dto;

import com.ilgiyebo.domain.exp.entity.RewardType;

import java.util.List;

public record LevelUpResultDto(
    int newLevel,
    List<RewardDto> rewards
) {
    public record RewardDto(
        RewardType type,
        String description
    ) {}
}
