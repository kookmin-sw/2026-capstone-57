package com.ilgiyebo.domain.interaction.dto;

import com.ilgiyebo.domain.interaction.entity.HintQuestionEntity;
import com.ilgiyebo.domain.interaction.entity.HintQuestionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record HintQuestionDto(
    UUID id,
    UUID matchId,
    UUID senderId,
    UUID responderId,
    String question,
    String answer,
    int quizIndex,
    HintQuestionStatus status,
    LocalDateTime createdAt
) {
    public static HintQuestionDto from(HintQuestionEntity entity) {
        return new HintQuestionDto(
                entity.getId(),
                entity.getMatch().getId(),
                entity.getSender().getId(),
                entity.getResponder().getId(),
                entity.getQuestion(),
                entity.getAnswer(),
                entity.getQuizIndex(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
