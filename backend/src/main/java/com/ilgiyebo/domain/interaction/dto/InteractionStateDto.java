package com.ilgiyebo.domain.interaction.dto;

import com.ilgiyebo.domain.interaction.entity.StageStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InteractionStateDto(
    UUID interactionId,
    UUID matchId,
    int currentStage,
    StageStatus stageStatus,
    LocalDate matchCycleStart,
    LocalDate matchCycleEnd,
    StageDataDto stageData
) {

    public sealed interface StageDataDto permits QuizData, ChatData, GameData, MissionData, ReviewData {}

    public record QuizData(List<String> quizCompletedBy) implements StageDataDto {}
    public record ChatData(Instant chatStartTime, Instant chatEndTime) implements StageDataDto {}
    public record GameData(String gameType, boolean gameCompleted) implements StageDataDto {}
    public record MissionData(UUID missionId, List<String> missionConfirmedBy, boolean extended) implements StageDataDto {}
    public record ReviewData(List<String> reviewCompletedBy) implements StageDataDto {}
}
