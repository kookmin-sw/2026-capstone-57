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

import java.util.UUID;

@Tag(name = "회고", description = "매칭 회고 작성 API")
@RestController
@RequestMapping("/api/interactions/{matchId}/review")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "회고 모드 선택",
            description = "AI 기반 또는 직접 작성 모드를 선택한다. AI 모드 시 첫 번째 질문도 함께 반환된다.")
    @PostMapping("/mode")
    public ResponseEntity<ReviewSessionResponse> selectReviewMode(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody SelectReviewModeRequest request) {
        return ResponseEntity.ok(reviewService.selectReviewMode(matchId, userId, request.mode()));
    }

    @Operation(summary = "AI 질문 답변 및 다음 질문 수신",
            description = "현재 질문에 답변하면 AI가 다음 질문을 생성한다. " +
                    "isConversationComplete=true이면 대화가 완료되어 generate를 호출할 수 있다.")
    @PostMapping("/answer")
    public ResponseEntity<NextQuestionResponse> answerAndGetNextQuestion(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @RequestParam UUID sessionId,
            @Valid @RequestBody AnswerTurnRequest request) {
        return ResponseEntity.ok(reviewService.answerAndGetNextQuestion(matchId, sessionId, userId, request.answer()));
    }

    @Operation(summary = "AI 회고 생성",
            description = "대화 히스토리 기반으로 AI가 회고 글을 생성한다.")
    @PostMapping("/generate")
    public ResponseEntity<GeneratedReviewPreview> generateReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @RequestParam UUID sessionId) {
        return ResponseEntity.ok(reviewService.generateReview(matchId, sessionId, userId));
    }

    @Operation(summary = "AI 회고 확정",
            description = "생성된 회고를 수정 및 확정한다.")
    @PostMapping("/confirm")
    public ResponseEntity<ReviewResponse> editGeneratedReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @RequestParam UUID sessionId,
            @Valid @RequestBody EditReviewRequest request) {
        return ResponseEntity.ok(reviewService.editGeneratedReview(matchId, sessionId, userId, request));
    }

    @Operation(summary = "직접 회고 제출",
            description = "직접 작성 모드로 회고를 제출한다.")
    @PostMapping("/direct")
    public ResponseEntity<ReviewResponse> submitDirectReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody DirectReviewInputDto review) {
        return ResponseEntity.ok(reviewService.submitDirectReview(matchId, userId, review));
    }

    @Operation(summary = "회고 조회",
            description = "본인의 회고를 조회한다. 회고가 없으면 204 No Content를 반환한다.")
    @GetMapping
    public ResponseEntity<ReviewResponse> getReview(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return reviewService.getReview(matchId, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
