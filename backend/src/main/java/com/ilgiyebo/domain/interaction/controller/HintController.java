package com.ilgiyebo.domain.interaction.controller;

import com.ilgiyebo.domain.interaction.dto.*;
import com.ilgiyebo.domain.interaction.service.HintQuestionService;
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

@Tag(name = "Hint", description = "매칭 힌트 관련 API")
@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class HintController {

    private final HintQuestionService hintQuestionService;

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
                hintQuestionService.answerHintQuestion(matchId, questionId, userId, request.answer()));
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