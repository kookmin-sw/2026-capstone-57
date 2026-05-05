package com.ilgiyebo.dto;

import com.ilgiyebo.domain.SlotEntity;
import com.ilgiyebo.domain.SlotPriority;
import com.ilgiyebo.domain.SlotStatus;
import jakarta.annotation.Nullable;

import java.util.UUID;

public record SlotResponseDto(
    UUID id,
    UUID userId,
    SlotPriority priority,
    UUID currentMatchId,
    boolean isQuickMatch,
    SlotStatus status,
    @Nullable MatchedUserDto matchedUser
) {
    public static SlotResponseDto from(SlotEntity entity) {
        return new SlotResponseDto(
            entity.getId(),
            entity.getUserId(),
            entity.getPriority(),
            entity.getCurrentMatchId(),
            entity.isQuickMatch(),
            entity.getStatus(),
            null
        );
    }

    public static SlotResponseDto from(SlotEntity entity, @Nullable MatchedUserDto matchedUser) {
        return new SlotResponseDto(
            entity.getId(),
            entity.getUserId(),
            entity.getPriority(),
            entity.getCurrentMatchId(),
            entity.isQuickMatch(),
            entity.getStatus(),
            matchedUser
        );
    }
}
