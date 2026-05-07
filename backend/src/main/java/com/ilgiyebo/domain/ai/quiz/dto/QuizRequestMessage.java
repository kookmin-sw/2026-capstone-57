package com.ilgiyebo.domain.ai.quiz.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuizRequestMessage(
    String action,
    UUID matchId,
    UUID requesterId,
    UUID targetUserId,
    Instant requestedAt,
    TargetProfile targetProfile
) {
    public record TargetProfile(
        String name,
        String nickname,
        String university,
        String major,
        List<String> hobbies,
        List<String> interests,
        List<String> personalityType
    ) {}
}
