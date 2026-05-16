package com.ilgiyebo.domain.review.controller;

import com.ilgiyebo.domain.review.dto.*;
import com.ilgiyebo.domain.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "회고", description = "매칭 회고 작성 API")
@RestController
@RequestMapping("/api/interactions/{matchId}/review")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "회고 모드 선택", description = "AI 기반 또는 직접 작성 모드를 선택한다")
    @PostMapping("/mode")
    public ResponseEntity<ReviewSessionResponse> selectReviewMode(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody SelectReviewModeRequest request) {
        return ResponseEntity.ok(reviewService.selectReviewMode(matchId, userId, request.mode()));
    }

    @Operation(summary = "AI 회고 질문 조회", description = "AI 기반 모드의 질문 목록을 조회한다")
    @GetMapping("/questions")
    public ResponseEntity<List<AiReviewQuestionDto>> getAIQuestions(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @RequestParam UUID sessionId) {
        return ResponseEntity.ok(reviewService.getAIQuestions(matchId, sessionId, userId));
    }

    @Operation(summary = "AI 질문 답변", description = "AI 회고 질문에 답변한다")
    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<AiReviewQuestionDto> answerAIQuestion(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @PathVariable UUID questionId,
            @RequestParam UUID sessionId,
            @Valid @RequestBody AnswerReviewQuestionRequest request) {
        return ResponseEntity.ok(reviewService.answerAIQuestion(matchId, sessionId, questionId, userId, request.answer()));
    }

    @Operation(summary = "AI 회고 생성", description = "답변 기반으로 AI가 회고 글을 생성한다")
    @PostMapping("/generate")
    public ResponseEntity<GeneratedReviewPreview> generateReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @RequestParam UUID sessionId) {
        return ResponseEntity.ok(reviewService.generateReview(matchId, sessionId, userId));
    }

    @Operation(summary = "AI 회고 확정", description = "생성된 회고를 수정 및 확정한다")
    @PostMapping("/confirm")
    public ResponseEntity<ReviewResponse> editGeneratedReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @RequestParam UUID sessionId,
            @Valid @RequestBody EditReviewRequest request) {
        return ResponseEntity.ok(reviewService.editGeneratedReview(matchId, sessionId, userId, request));
    }

    @Operation(summary = "직접 회고 제출", description = "직접 작성 모드로 회고를 제출한다")
    @PostMapping("/direct")
    public ResponseEntity<ReviewResponse> submitDirectReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody DirectReviewInputDto review) {
        return ResponseEntity.ok(reviewService.submitDirectReview(matchId, userId, review));
    }

    @Operation(summary = "회고 조회", description = "본인의 회고를 조회한다. 회고가 없으면 204 No Content를 반환한다")
    @GetMapping
    public ResponseEntity<ReviewResponse> getReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return reviewService.getReview(matchId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
