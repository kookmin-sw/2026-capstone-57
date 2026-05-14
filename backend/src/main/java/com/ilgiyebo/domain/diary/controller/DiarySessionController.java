package com.ilgiyebo.domain.diary.controller;

import com.ilgiyebo.domain.diary.dto.*;
import com.ilgiyebo.domain.diary.service.DiarySessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "AI 일기 세션", description = "AI 기반 멀티턴 대화 일기 작성 API")
@RestController
@RequestMapping("/api/diary/sessions")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class DiarySessionController {

    private final DiarySessionService diarySessionService;

    @Operation(summary = "AI 일기 세션 시작", description = "AI 일기 멀티턴 대화 세션을 시작한다")
    @PostMapping("/start")
    public ResponseEntity<DiarySessionResponse> startSession(
            @AuthenticationPrincipal UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(diarySessionService.startAISession(userId, date));
    }

    @Operation(summary = "질문에 답변", description = "현재 질문에 답변하고 다음 질문 또는 대화 완료를 받는다")
    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<DiaryTurnResponse> answerQuestion(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody DiaryAnswerRequest request) {
        return ResponseEntity.ok(diarySessionService.answerQuestion(sessionId, userId, request.answer()));
    }

    @Operation(summary = "일기 생성 요청", description = "모든 답변을 기반으로 AI에게 일기 생성을 요청한다")
    @PostMapping("/{sessionId}/generate")
    public ResponseEntity<GeneratedDiaryPreview> generateDiary(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(diarySessionService.generateDiary(sessionId, userId));
    }

    @Operation(summary = "일기 확정", description = "생성된 일기를 확정한다 (선택적 수정 포함)")
    @PostMapping("/{sessionId}/confirm")
    public ResponseEntity<DiaryEntryResponse> confirmDiary(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId,
            @RequestBody DiaryConfirmRequest request) {
        return ResponseEntity.ok(diarySessionService.confirmDiary(
                sessionId, userId, request.editedContent(), request.emotionTag()));
    }

    @Operation(summary = "활성 세션 조회", description = "특정 날짜의 진행 중인 AI 일기 세션을 조회한다")
    @GetMapping("/active")
    public ResponseEntity<DiarySessionResponse> getActiveSession(
            @AuthenticationPrincipal UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return diarySessionService.getActiveSession(userId, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "세션 취소", description = "진행 중인 AI 일기 세션을 취소한다")
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<Void> cancelSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId) {
        diarySessionService.cancelSession(sessionId, userId);
        return ResponseEntity.ok().build();
    }
}
