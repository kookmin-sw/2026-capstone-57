package com.ilgiyebo.domain.interaction.controller;

import com.ilgiyebo.domain.interaction.dto.*;
import com.ilgiyebo.domain.interaction.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Interaction", description = "매칭 상호작용 단계 관리 API")
@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @Operation(summary = "현재 상호작용 상태 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{matchId}")
    public ResponseEntity<InteractionStateDto> getInteractionState(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(interactionService.getInteractionState(matchId, userId));
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
}