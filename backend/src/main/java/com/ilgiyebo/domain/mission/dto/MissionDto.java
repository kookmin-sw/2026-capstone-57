package com.ilgiyebo.domain.mission.dto;

import com.ilgiyebo.domain.mission.entity.MissionEntity;
import com.ilgiyebo.domain.mission.entity.MissionStatus;

import java.time.Instant;
import java.util.List;

/**
 * 미션 응답 DTO.
 */
public record MissionDto(
    String id,
    String matchId,
    String location,
    String activity,
    String description,
    Instant deadline,
    List<String> confirmedBy,
    MissionStatus status,
    String dayOfWeek,
    String timeSlot,
    String selectedNodeId
) {

    public static MissionDto from(MissionEntity entity) {
        return new MissionDto(
            entity.getId().toString(),
            entity.getMatch().getId().toString(),
            entity.getLocation(),
            entity.getActivity(),
            entity.getDescription(),
            entity.getDeadline(),
            entity.getConfirmedBy(),
            entity.getStatus(),
            entity.getDayOfWeek(),
            entity.getTimeSlot(),
            entity.getSelectedNodeId()
        );
    }
}
