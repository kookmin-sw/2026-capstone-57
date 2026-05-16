package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.HintQuestionDto;

import java.util.List;
import java.util.UUID;

public interface HintQuestionService {

    /** 힌트 질문 전송 (퀴즈 단계에서만 가능) */
    HintQuestionDto sendHintQuestion(UUID matchId, UUID senderId, String question, int quizIndex);

    /** 힌트 질문 답변 처리 */
    HintQuestionDto answerHintQuestion(UUID matchId, UUID questionId, UUID responderId, String answer);

    /** 힌트 질문/답변 목록 조회 */
    List<HintQuestionDto> getHintQuestions(UUID matchId, UUID userId);
}