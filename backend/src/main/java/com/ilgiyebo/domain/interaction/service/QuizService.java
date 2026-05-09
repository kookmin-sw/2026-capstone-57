package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.QuizQuestionResponse;
import com.ilgiyebo.domain.interaction.dto.QuizResponseDto;
import com.ilgiyebo.domain.interaction.dto.QuizSubmitRequest;

import java.util.List;
import java.util.UUID;

public interface QuizService {

    /** 퀴즈 문항 조회 (정답 제외) */
    List<QuizQuestionResponse> getQuiz(UUID matchId, UUID userId);

    /** 퀴즈 답안 제출 및 채점 */
    QuizResponseDto submitQuiz(UUID matchId, UUID userId, QuizSubmitRequest request);
}