package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.InteractionStateDto;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionDto;
import com.ilgiyebo.domain.interaction.entity.TerminationReason;

import java.util.List;
import java.util.UUID;

public interface InteractionService {

    /** 현재 상호작용 상태 조회 */
    InteractionStateDto getInteractionState(UUID matchId, UUID userId);

    /** 매칭 종료 (거절, 기한 만료, 신고 등) */
    void terminateMatch(UUID matchId, UUID userId, TerminationReason reason);

    /** 퀴즈 완료 처리 (AIService에서 호출) */
    InteractionStateDto completeQuiz(UUID matchId, UUID userId);

    /** 퀴즈 데이터 저장 (요청자 기준으로 해당 유저의 퀴즈 슬롯에 저장) */
    void storeQuizData(UUID matchId, UUID requesterId, List<QuizQuestionDto> quizData);
}