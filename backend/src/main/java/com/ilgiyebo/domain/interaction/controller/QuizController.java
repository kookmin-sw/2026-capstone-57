package com.ilgiyebo.domain.interaction.controller;

import com.ilgiyebo.domain.interaction.dto.*;
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

@Tag(name = "Quiz", description = "매칭 퀴즈 관련 API")
@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class QuizController {

    private final InteractionService interactionService;
    private final QuizService quizService;

    @Operation(summary = "퀴즈 단계 완료 처리")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/quiz/complete")
    public ResponseEntity<InteractionStateDto> completeQuiz(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(interactionService.completeQuiz(matchId, userId));
    }

    @Operation(summary = "퀴즈 문항 조회 (정답 미포함)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{matchId}/quiz")
    public ResponseEntity<List<QuizQuestionResponse>> getQuiz(
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
}
