package com.ilgiyebo.domain.mission.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * AI 미션 생성 요청 SQS 메시지.
 * 백엔드 → AI 서버 방향으로 발행된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MissionGenerateRequestMessage(
    String action,
    String matchId,
    String userAId,
    String userBId,
    String timeSlot,
    UserRouteInfo userARoute,
    UserRouteInfo userBRoute,
    Instant requestedAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserRouteInfo(
        BuildingNode fromBuilding,
        BuildingNode toBuilding,
        List<String> subNodeIds
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BuildingNode(
        String id,
        String name
    ) {}

    public static MissionGenerateRequestMessage of(
            String matchId,
            String userAId,
            String userBId,
            String timeSlot,
            UserRouteInfo userARoute,
            UserRouteInfo userBRoute) {
        return new MissionGenerateRequestMessage(
            "GENERATE_MISSION",
            matchId,
            userAId,
            userBId,
            timeSlot,
            userARoute,
            userBRoute,
            Instant.now()
        );
    }
}
