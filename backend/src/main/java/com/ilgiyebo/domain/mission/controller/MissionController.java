package com.ilgiyebo.domain.mission.controller;

import com.ilgiyebo.domain.mission.dto.MissionDto;
import com.ilgiyebo.domain.mission.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Mission", description = "4단계 미션 기반 만남 API")
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @Operation(summary = "미션 조회", description = "매칭 ID로 현재 미션 정보를 조회한다")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{matchId}")
    public ResponseEntity<MissionDto> getMission(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(missionService.getMission(matchId, userId));
    }

    @Operation(summary = "미션 수행 확인", description = "미션 수행을 확인한다. 양쪽 모두 확인 시 5단계(회고)가 해금된다")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/confirm")
    public ResponseEntity<MissionDto> confirmMission(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(missionService.confirmMission(matchId, userId));
    }

    @Operation(summary = "미션 생성 요청 (관리자/테스트용)", description = "매칭에 대한 미션 생성을 수동으로 요청한다")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{matchId}/generate")
    public ResponseEntity<Void> requestMissionGeneration(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        missionService.requestMissionGeneration(matchId);
        return ResponseEntity.accepted().build();
    }
}
