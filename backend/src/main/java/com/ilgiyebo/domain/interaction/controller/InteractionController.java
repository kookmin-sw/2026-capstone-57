package com.ilgiyebo.domain.interaction.controller;

import com.ilgiyebo.domain.interaction.dto.*;
import com.ilgiyebo.domain.interaction.service.HintQuestionService;
import com.ilgiyebo.domain.interaction.service.InteractionService;
import com.ilgiyebo.domain.interaction.service.QuizService;
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

@Tag(name = "Interaction", description = "매칭 상호작용 단계 관리 API")
@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;
    private final QuizService quizService;
    private final HintQuestionService hintQuestionService;

    @Operation(summary = "현재 상호작용 상태 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{matchId}")
    public ResponseEntity<InteractionStateDto> getInteractionState(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(interactionService.getInteractionState(matchId, userId));
    }

    @Operation(summary = "다음 단계 진행 수락 및 거절")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/advance")
    public ResponseEntity<InteractionStateDto> respondToStageAdvance(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody StageAdvanceRequest request) {
        return ResponseEntity.ok(
                interactionService.respondToStageAdvance(matchId, userId, request.accept()));
    }

    @Operation(summary = "매칭 강제 종료")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/terminate")
    public ResponseEntity<Void> terminateMatch(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody TerminateMatchRequest request) {
        interactionService.terminateMatch(matchId, request.reason());
        return ResponseEntity.noContent().build();
    }

    // --- Quiz API ---

    @Operation(summary = "퀴즈 단계 완료 처리")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/quiz/complete")
    public ResponseEntity<InteractionStateDto> completeQuiz(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(interactionService.completeQuiz(matchId, userId));
    }

    @Operation(summary = "퀴즈 문항 조회 (AI 기반 생성)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{matchId}/quiz")
    public ResponseEntity<List<QuizQuestionDto>> getQuiz(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(quizService.getQuiz(matchId, userId));
    }

    @Operation(summary = "퀴즈 답안 제출 및 채점")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/quiz/submit")
    public ResponseEntity<QuizResponseDto> submitQuiz(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody QuizSubmitRequest request) {
        return ResponseEntity.ok(quizService.submitQuiz(matchId, userId, request));
    }

    // --- Hint Question API ---

    @Operation(summary = "힌트 질문 전송")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/hints")
    public ResponseEntity<HintQuestionDto> sendHintQuestion(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody SendHintQuestionRequest request) {
        return ResponseEntity.ok(
                hintQuestionService.sendHintQuestion(matchId, userId, request.question()));
    }

    @Operation(summary = "힌트 질문에 대한 답변 등록")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/hints/{questionId}/answer")
    public ResponseEntity<HintQuestionDto> answerHintQuestion(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @PathVariable UUID questionId,
            @Valid @RequestBody AnswerHintQuestionRequest request) {
        return ResponseEntity.ok(
                hintQuestionService.answerHintQuestion(questionId, userId, request.answer()));
    }

    @Operation(summary = "힌트 질문 및 답변 목록 전체 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{matchId}/hints")
    public ResponseEntity<List<HintQuestionDto>> getHintQuestions(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(hintQuestionService.getHintQuestions(matchId, userId));
    }
}