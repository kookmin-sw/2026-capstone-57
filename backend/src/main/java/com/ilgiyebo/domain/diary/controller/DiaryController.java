package com.ilgiyebo.domain.diary.controller;

import com.ilgiyebo.domain.diary.dto.*;
import com.ilgiyebo.domain.diary.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "일기", description = "일기 CRUD API")
@RestController
@RequestMapping("/api/diary")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "일기 작성", description = "일기를 작성한다 (같은 날짜에 이미 존재하면 업데이트)")
    @PostMapping
    public ResponseEntity<DiaryEntryResponse> createEntry(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody DiaryInputDto input) {
        return ResponseEntity.ok(diaryService.createEntry(userId, input));
    }

    @Operation(summary = "일기 목록 조회", description = "본인의 일기 목록을 페이지네이션으로 조회한다")
    @GetMapping
    public ResponseEntity<Page<DiaryEntryResponse>> getEntries(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 20, sort = "entryDate") Pageable pageable) {
        return ResponseEntity.ok(diaryService.getEntries(userId, pageable));
    }

    @Operation(summary = "연속 작성 일수 조회", description = "현재 연속 작성 일수와 최장 연속 작성 일수를 조회한다")
    @GetMapping("/streak")
    public ResponseEntity<StreakInfoDto> getStreak(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(diaryService.getStreak(userId));
    }

    @Operation(summary = "감정 변화 추이 조회", description = "기간별 감정 변화 추이를 조회한다 (week, month, quarter, year)")
    @GetMapping("/emotion-trend")
    public ResponseEntity<List<EmotionTrendDto>> getEmotionTrend(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(diaryService.getEmotionTrend(userId, period));
    }
}
