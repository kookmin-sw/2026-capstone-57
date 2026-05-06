package com.ilgiyebo.dto;

import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.MatchStatus;
import com.ilgiyebo.domain.MissionEntity;
import com.ilgiyebo.domain.MissionStatus;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminMatchDetailDto(
    UUID matchId,
    MatchStatus status,
    LocalDate cycleStartDate,
    LocalDate cycleEndDate,
    UserSummaryDto userA,
    UserSummaryDto userB,
    @Nullable MissionSummaryDto mission
) {

    public record UserSummaryDto(UUID id, String nickname) {}

    public record MissionSummaryDto(
        UUID id,
        String location,
        String activity,
        String description,
        Instant deadline,
        MissionStatus status
    ) {
        public static MissionSummaryDto from(MissionEntity entity) {
            return new MissionSummaryDto(
                entity.getId(),
                entity.getLocation(),
                entity.getActivity(),
                entity.getDescription(),
                entity.getDeadline(),
                entity.getStatus()
            );
        }
    }

    public static AdminMatchDetailDto from(MatchEntity match, @Nullable MissionEntity mission) {
        return new AdminMatchDetailDto(
            match.getId(),
            match.getStatus(),
            match.getCycleStartDate(),
            match.getCycleEndDate(),
            new UserSummaryDto(match.getUserA().getId(), match.getUserA().getNickname()),
            new UserSummaryDto(match.getUserB().getId(), match.getUserB().getNickname()),
            mission != null ? MissionSummaryDto.from(mission) : null
        );
    }
}
