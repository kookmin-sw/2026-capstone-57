package com.ilgiyebo.domain.interaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AI 퀴즈 생성 요청 SQS 메시지.
 * 회원가입 시 유저 프로필 기반으로 퀴즈를 생성 요청한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizGenerateRequestMessage(
    String action,
    UUID userId,
    Instant requestedAt,
    UserProfile userProfile
) {

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserProfile(
            String name,
            String nickname,
            String university,
            String major,
            List<String> hobbies,
            List<String> interests,
            String personalityType
    ) {}

    public static QuizGenerateRequestMessage of(UUID userId, UserProfile userProfile) {
        return new QuizGenerateRequestMessage(
            "GENERATE_QUIZ",
            userId,
            Instant.now(),
            userProfile
        );
    }
}
