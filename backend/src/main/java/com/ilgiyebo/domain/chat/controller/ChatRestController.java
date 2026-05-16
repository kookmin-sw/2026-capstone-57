package com.ilgiyebo.domain.chat.controller;

import com.ilgiyebo.domain.chat.dto.ChatMessageDto;
import com.ilgiyebo.domain.chat.dto.ChatSessionCreateRequest;
import com.ilgiyebo.domain.chat.dto.ChatSessionDto;
import com.ilgiyebo.domain.chat.entity.ChatMessageEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.service.ChatMessageService;
import com.ilgiyebo.domain.chat.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Tag(name = "채팅", description = "채팅 세션 및 메시지 조회 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ChatRestController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    @Operation(summary = "매치별 채팅 세션 조회", description = "매치 ID로 채팅 세션 정보를 조회한다")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/sessions/match/{matchId}")
    public ResponseEntity<ChatSessionDto> getSessionByMatchId(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID matchId) {
        chatSessionService.validateParticipant(userId, matchId);
        ChatSessionEntity session = chatSessionService.getSessionByMatchId(matchId);
        return ResponseEntity.ok(toSessionDto(session));
    }

    @Operation(summary = "채팅 메시지 목록 조회", description = "세션 ID로 채팅 메시지 목록을 조회한다")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessagesBySessionId(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId) {
        ChatSessionEntity session = chatSessionService.getSessionById(sessionId);
        chatSessionService.validateParticipant(userId, session.getMatchId());
        List<ChatMessageEntity> messages = chatMessageService.getMessagesBySessionId(sessionId);
        return ResponseEntity.ok(messages.stream().map(this::toMessageDto).toList());
    }

    @Operation(summary = "채팅 세션 생성", description = "매치 ID로 새 채팅 세션을 생성한다")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDto> createSession(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ChatSessionCreateRequest request) {
        chatSessionService.validateParticipant(userId, request.matchId());
        ChatSessionEntity session = chatSessionService.createSession(request.matchId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toSessionDto(session));
    }

    private ChatSessionDto toSessionDto(ChatSessionEntity entity) {
        return ChatSessionDto.builder()
                .sessionId(entity.getId())
                .matchId(entity.getMatchId())
                .startTime(entity.getStartTime())
                .status(entity.getStatus())
                .tokenLimit(entity.getTokenLimit())
                .usedTokens(entity.getUsedTokens())
                .icebreakerQuestion(entity.getIcebreakerQuestion())
                .build();
    }

    private ChatMessageDto toMessageDto(ChatMessageEntity entity) {
        return ChatMessageDto.builder()
                .messageId(entity.getId())
                .sessionId(entity.getSessionId())
                .senderId(entity.getSenderId())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt() != null
                        ? entity.getCreatedAt().toInstant(ZoneOffset.UTC)
                        : null)
                .build();
    }
}
