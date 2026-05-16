package com.ilgiyebo.domain.review.service;

import com.ilgiyebo.domain.review.dto.*;
import com.ilgiyebo.domain.review.entity.ReviewMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
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

    /** 매칭의 회고 조회 (본인만) */
    Optional<ReviewResponse> getReview(UUID matchId, UUID userId);

    /**
     * 본인이 작성한 회고 목록을 필터링 옵션과 함께 페이징 조회한다 (최신순).
     *
     * @param userId           조회 대상 사용자 ID
     * @param mode             회고 모드 필터 (nullable)
     * @param minSatisfaction  만족도 하한 필터 (nullable, 1~5)
     * @param maxSatisfaction  만족도 상한 필터 (nullable, 1~5)
     * @param fromDate         시작 일시 필터 (nullable)
     * @param toDate           종료 일시 필터 (nullable)
     * @param pageable         페이지 정보
     */
    Page<ReviewResponse> getMyReviews(
            UUID userId,
            ReviewMode mode,
            Integer minSatisfaction,
            Integer maxSatisfaction,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable);
}
