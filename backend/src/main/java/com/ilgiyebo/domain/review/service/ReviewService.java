package com.ilgiyebo.domain.review.service;

import com.ilgiyebo.domain.review.dto.*;
import com.ilgiyebo.domain.review.entity.ReviewMode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewService {

    /** 회고 작성 모드 선택 (AI 기반 / 직접 작성) */
    ReviewSessionResponse selectReviewMode(UUID matchId, UUID userId, ReviewMode mode);

    /** AI 질문 조회 (AI 기반 모드) */
    List<AiReviewQuestionDto> getAIQuestions(UUID matchId, UUID sessionId, UUID userId);

    /** AI 질문에 답변 */
    AiReviewQuestionDto answerAIQuestion(UUID matchId, UUID sessionId, UUID questionId, UUID userId, String answer);

    /** AI가 답변 기반으로 회고 글 생성 */
    GeneratedReviewPreview generateReview(UUID matchId, UUID sessionId, UUID userId);

    /** 생성된 회고 글 수정 및 확정 */
    ReviewResponse editGeneratedReview(UUID matchId, UUID sessionId, UUID userId, EditReviewRequest request);

    /** 직접 작성 모드로 회고 제출 */
    ReviewResponse submitDirectReview(UUID matchId, UUID userId, DirectReviewInputDto review);

    /** 회고 조회 (본인만) */
    Optional<ReviewResponse> getReview(UUID matchId, UUID userId);
}
