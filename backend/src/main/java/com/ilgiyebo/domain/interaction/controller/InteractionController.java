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

@Tag(name = "Interaction", description = "Matching interaction stage management API")
@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;
    private final QuizService quizService;
    private final HintQuestionService hintQuestionService;

    @Operation(summary = "Get interaction state")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{matchId}")
    public ResponseEntity<InteractionStateDto> getInteractionState(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(interactionService.getInteractionState(matchId, userId));
    }

    @Operation(summary = "Accept or reject stage advance")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/advance")
    public ResponseEntity<InteractionStateDto> respondToStageAdvance(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody StageAdvanceRequest request) {
        return ResponseEntity.ok(
            interactionService.respondToStageAdvance(matchId, userId, request.accept()));
    }

    @Operation(summary = "Terminate match")
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

    @Operation(summary = "Complete quiz stage")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/quiz/complete")
    public ResponseEntity<InteractionStateDto> completeQuiz(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(interactionService.completeQuiz(matchId, userId));
    }

    @Operation(summary = "Get quiz questions")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{matchId}/quiz")
    public ResponseEntity<List<QuizQuestionDto>> getQuiz(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(quizService.getQuiz(matchId, userId));
    }

    @Operation(summary = "Submit quiz answers")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/quiz/submit")
    public ResponseEntity<QuizResponseDto> submitQuiz(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody QuizSubmitRequest request) {
        return ResponseEntity.ok(quizService.submitQuiz(matchId, userId, request));
    }

    // --- Hint Question API ---

    @Operation(summary = "Send hint question")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/hints")
    public ResponseEntity<HintQuestionDto> sendHintQuestion(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId,
            @Valid @RequestBody SendHintQuestionRequest request) {
        return ResponseEntity.ok(
            hintQuestionService.sendHintQuestion(matchId, userId, request.question()));
    }

    @Operation(summary = "Answer hint question")
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

    @Operation(summary = "Get hint questions list")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{matchId}/hints")
    public ResponseEntity<List<HintQuestionDto>> getHintQuestions(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(hintQuestionService.getHintQuestions(matchId, userId));
    }
}
