package com.ilgiyebo.domain.review.dto;

import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.review.entity.ReviewEntity;
import com.ilgiyebo.domain.review.entity.ReviewMode;
import com.ilgiyebo.domain.user.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID interactionId,
    UUID matchId,
    UUID userId,
    UUID opponentId,
    String opponentNickname,
    String opponentUniversity,
    ReviewMode mode,
    int satisfaction,
    String reflection,
    boolean wantToMeetAgain,
    boolean aiGenerated,
    LocalDateTime createdAt
) {
    /**
     * ReviewEntity로부터 응답 DTO를 생성한다.
     * 호출자는 반드시 트랜잭션 내에서 호출해야 한다 (interaction.match.userA/userB LAZY 프록시 초기화 필요).
     */
    public static ReviewResponse from(ReviewEntity entity) {
        MatchEntity match = entity.getInteraction().getMatch();
        UUID reviewerId = entity.getUser().getId();

        UserEntity opponent = match.getUserA().getId().equals(reviewerId)
                ? match.getUserB()
                : match.getUserA();

        return new ReviewResponse(
            entity.getId(),
            entity.getInteraction().getId(),
            match.getId(),
            reviewerId,
            opponent.getId(),
            opponent.getNickname(),
            opponent.getUniversity(),
            entity.getMode(),
            entity.getSatisfaction(),
            entity.getReflection(),
            entity.isWantToMeetAgain(),
            entity.isAiGenerated(),
            entity.getCreatedAt()
        );
    }
}
