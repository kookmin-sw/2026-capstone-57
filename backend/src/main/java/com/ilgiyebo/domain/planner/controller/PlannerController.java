package com.ilgiyebo.domain.planner.controller;

import com.ilgiyebo.domain.planner.dto.CreatePlanEntryRequest;
import com.ilgiyebo.domain.planner.dto.PlanEntryResponse;
import com.ilgiyebo.domain.planner.dto.UpdatePlanEntryRequest;
import com.ilgiyebo.domain.planner.service.PlannerService;
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
import java.util.List;
import java.util.UUID;

@Tag(name = "플래너", description = "일정 CRUD API")
@RestController
@RequestMapping("/api/planner")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;

    @Operation(summary = "일정 생성", description = "날짜 기반 단일 일정을 생성한다 (source=MANUAL)")
    @PostMapping
    public ResponseEntity<PlanEntryResponse> createPlanEntry(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreatePlanEntryRequest request) {
        return ResponseEntity.ok(plannerService.createPlanEntry(userId, request));
    }

    @Operation(summary = "일정 수정", description = "일정을 수정한다 (MANUAL, SCHEDULE_AUTO 모두 수정 가능)")
    @PutMapping("/{entryId}")
    public ResponseEntity<PlanEntryResponse> updatePlanEntry(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID entryId,
            @Valid @RequestBody UpdatePlanEntryRequest request) {
        return ResponseEntity.ok(plannerService.updatePlanEntry(userId, entryId, request));
    }

    @Operation(summary = "일정 삭제", description = "일정을 삭제한다")
    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> deletePlanEntry(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID entryId) {
        plannerService.deletePlanEntry(userId, entryId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "일정 목록 조회", description = "특정 날짜의 일정 목록을 조회한다 (본인만)")
    @GetMapping
    public ResponseEntity<List<PlanEntryResponse>> getPlanEntries(
            @AuthenticationPrincipal UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(plannerService.getPlanEntries(userId, date));
    }
}
