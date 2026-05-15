package com.ilgiyebo.domain.exp.dto;

import com.ilgiyebo.domain.exp.entity.ExpActivity;
import com.ilgiyebo.domain.exp.entity.ExpHistoryEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExpHistoryEntryDto(
    UUID id,
    ExpActivity activity,
    int amount,
    int bonusAmount,
    LocalDateTime createdAt
) {
    public static ExpHistoryEntryDto from(ExpHistoryEntity entity) {
        return new ExpHistoryEntryDto(
            entity.getId(),
            entity.getActivity(),
            entity.getAmount(),
            entity.getBonusAmount(),
            entity.getCreatedAt()
        );
    }
}
