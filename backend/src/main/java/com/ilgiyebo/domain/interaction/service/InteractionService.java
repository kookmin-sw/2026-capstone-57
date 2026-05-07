package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.InteractionStateDto;
import com.ilgiyebo.domain.interaction.entity.TerminationReason;

import java.util.UUID;

public interface InteractionService {

    /** 현재 상호작용 상태 조회 */
    InteractionStateDto getInteractionState(UUID matchId, UUID userId);

    /** 매칭 종료 (거절, 기한 만료, 신고 등) */
    void terminateMatch(UUID matchId, TerminationReason reason);

    /** 퀴즈 완료 처리 (AIService에서 호출) */
    InteractionStateDto completeQuiz(UUID matchId, UUID userId);

    /** SQS를 통해 AI가 생성한 퀴즈 데이터를 저장 */
    void storeQuizData(UUID matchId, java.util.List<com.ilgiyebo.domain.interaction.dto.QuizQuestionDto> quizData);
}