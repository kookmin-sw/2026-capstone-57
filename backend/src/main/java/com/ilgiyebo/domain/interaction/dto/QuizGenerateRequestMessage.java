package com.ilgiyebo.domain.interaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AI quiz generation request SQS message.
 * Published from interaction to AI direction.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizGenerateRequestMessage(
    String action,
    UUID matchId,
    UUID requesterId,
    UUID targetUserId,
    Instant requestedAt,
    TargetProfile targetProfile
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TargetProfile(
        String name,
        String nickname,
        String university,
        String major,
        List<String> hobbies,
        List<String> interests,
        List<String> personalityType
    ) {}

    public static QuizGenerateRequestMessage of(
            UUID matchId, UUID requesterId, UUID targetUserId, TargetProfile targetProfile) {
        return new QuizGenerateRequestMessage(
            "GENERATE_QUIZ",
            matchId,
            requesterId,
            targetUserId,
            Instant.now(),
            targetProfile
        );
    }
}
