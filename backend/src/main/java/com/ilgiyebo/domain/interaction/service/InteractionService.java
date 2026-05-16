package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.InteractionStateDto;
import com.ilgiyebo.domain.interaction.entity.TerminationReason;

import java.util.UUID;

public interface InteractionService {

    /** 현재 상호작용 상태 조회 */
    InteractionStateDto getInteractionState(UUID matchId, UUID userId);

    /** 매칭 종료 (거절, 기한 만료, 신고 등) */
    void terminateMatch(UUID matchId, UUID userId, TerminationReason reason);

    /** 퀴즈 완료 처리 (AIService에서 호출) */
    InteractionStateDto completeQuiz(UUID matchId, UUID userId);

    /** 채팅 완료 처리 (토큰 한도 도달 시 호출) */
    InteractionStateDto completeChat(UUID matchId);

    /** 게임 완료 처리 (3단계 → 4단계 진행) */
    void completeGame(UUID matchId, String gameType);
}