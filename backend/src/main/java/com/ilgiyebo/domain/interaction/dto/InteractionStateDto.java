package com.ilgiyebo.domain.interaction.dto;

import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
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

    // 서비스에서 이리로 옮겨온 from 메서드
    public static InteractionStateDto from(InteractionEntity interaction, MatchEntity match) {
        return new InteractionStateDto(
                interaction.getId(),
                interaction.getMatch().getId(),
                interaction.getCurrentStage(),
                interaction.getStageStatus(),
                match.getCycleStartDate(),
                match.getCycleEndDate(),
                buildStageData(interaction)
        );
    }

    private static StageDataDto buildStageData(InteractionEntity interaction) {
        return switch (interaction.getCurrentStage()) {
            case 1 -> new QuizData(interaction.getQuizCompletedBy() != null ? interaction.getQuizCompletedBy() : List.of());
            case 2 -> new ChatData(interaction.getChatStartTime(), interaction.getChatEndTime());
            case 3 -> new GameData(interaction.getGameType(), interaction.isGameCompleted());
            case 4 -> new MissionData(interaction.getMissionId(),
                    interaction.getMissionConfirmedBy() != null ? interaction.getMissionConfirmedBy() : List.of(),
                    interaction.isMissionExtended());
            case 5 -> new ReviewData(interaction.getReviewCompletedBy() != null ? interaction.getReviewCompletedBy() : List.of());
            default -> new QuizData(List.of());
        };
    }

    public sealed interface StageDataDto permits QuizData, ChatData, GameData, MissionData, ReviewData {}

    public record QuizData(List<String> quizCompletedBy) implements StageDataDto {}
    public record ChatData(Instant chatStartTime, Instant chatEndTime) implements StageDataDto {}
    public record GameData(String gameType, boolean gameCompleted) implements StageDataDto {}
    public record MissionData(UUID missionId, List<String> missionConfirmedBy, boolean extended) implements StageDataDto {}
    public record ReviewData(List<String> reviewCompletedBy) implements StageDataDto {}
}
