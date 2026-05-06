package com.ilgiyebo.domain.interaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AI 퀴즈 생성 요청 SQS 메시지.
 * interaction → AI 방향으로 발행된다.
 *
 * 예시:
 * {
 *   "action": "GENERATE_QUIZ",
 *   "matchId": "550e8400-e29b-41d4-a716-446655440000",
 *   "requesterId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
 *   "targetUserId": "550e8400-e29b-41d4-a716-446655440001",
 *   "requestedAt": "2026-05-05T09:30:00.000Z",
 *   "targetProfile": {
 *     "name": "김철수",
 *     "nickname": "철수",
 *     "university": "국민대학교",
 *     "major": "컴퓨터공학과",
 *     "hobbies": ["요리", "게임", "등산"],
 *     "interests": ["AI", "웹개발"],
 *     "personalityType": ["INTJ", "분석적"]
 *   }
 * }
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
