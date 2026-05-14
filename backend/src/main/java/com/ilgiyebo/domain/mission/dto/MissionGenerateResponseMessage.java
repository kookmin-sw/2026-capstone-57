package com.ilgiyebo.domain.mission.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * AI 미션 생성 응답 SQS 메시지.
 * AI 서버 → 백엔드 방향으로 수신된다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MissionGenerateResponseMessage(
    String action,
    String status,
    String matchId,
    MissionPayload mission,
    Instant completedAt,
    String errorMessage
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MissionPayload(
        String location,
        String activity,
        String description,
        String selectedNodeId
    ) {}
}
