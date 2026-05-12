package com.ilgiyebo.domain.user.controller;

import com.ilgiyebo.domain.user.dto.ScheduleResponse;
import com.ilgiyebo.domain.user.dto.ScheduleUpsertResponse;
import com.ilgiyebo.domain.user.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "스케줄", description = "에브리타임 시간표 연동 API")
@RestController
@RequestMapping("/api/users/me/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "내 시간표 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ScheduleResponse> getMySchedule(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(scheduleService.getMySchedule(userId));
    }

    @Operation(summary = "에브리타임 시간표 등록", description = "에브리타임 identifier로 시간표를 가져와 등록한다")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{identifier}")
    public ResponseEntity<ScheduleUpsertResponse> upsertMySchedule(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String identifier
    ) {
        return ResponseEntity.ok(scheduleService.upsertMySchedule(userId, identifier));
    }
}
