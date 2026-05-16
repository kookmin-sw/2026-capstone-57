package com.ilgiyebo.domain.game.controller;

import com.ilgiyebo.domain.game.dto.response.GameSessionResponse;
import com.ilgiyebo.domain.game.service.GameSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Game Session", description = "게임 세션 관리 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GameSessionController {

    private final GameSessionService gameSessionService;

    @Operation(summary = "게임 세션 생성")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/matches/{matchId}/game-sessions")
    public ResponseEntity<GameSessionResponse> createSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        GameSessionResponse response = gameSessionService.createSession(matchId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게임 세션 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/game-sessions/{gameSessionId}")
    public ResponseEntity<GameSessionResponse> getSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID gameSessionId) {
        GameSessionResponse response = gameSessionService.getSession(gameSessionId, userId);
        return ResponseEntity.ok(response);
    }
}
