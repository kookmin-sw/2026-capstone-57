package com.ilgiyebo.domain.chat.controller;

import com.ilgiyebo.common.exception.BusinessException;
import com.ilgiyebo.domain.chat.dto.ChatMessageDto;
import com.ilgiyebo.domain.chat.dto.ChatSendRequest;
import com.ilgiyebo.domain.chat.entity.ChatMessageEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.service.ChatMessageService;
import com.ilgiyebo.domain.chat.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ChatSessionService chatSessionService;

    @MessageMapping("/chat/{sessionId}/send")
    @SendTo("/topic/chat/{sessionId}")
    public ChatMessageDto sendMessage(
            @DestinationVariable UUID sessionId,
            @Valid ChatSendRequest request,
            Principal principal) {
        UUID userId = extractUserId(principal);

        // Validate participant
        ChatSessionEntity session = chatSessionService.getSessionById(sessionId);
        chatSessionService.validateParticipant(userId, session.getMatchId());

        // Save and broadcast
        ChatMessageEntity saved = chatMessageService.saveMessage(sessionId, userId, request.content());

        return ChatMessageDto.builder()
                .messageId(saved.getId())
                .sessionId(saved.getSessionId())
                .senderId(saved.getSenderId())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt() != null
                        ? saved.getCreatedAt().toInstant(ZoneOffset.UTC)
                        : null)
                .build();
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public String handleBusinessException(BusinessException ex) {
        log.warn("Chat message error: {}", ex.getMessage());
        return ex.getMessage();
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public String handleException(Exception ex) {
        log.error("Unexpected chat error", ex);
        return "메시지 전송 중 오류가 발생했습니다";
    }

    private UUID extractUserId(Principal principal) {
        // The StompChannelInterceptor sets UsernamePasswordAuthenticationToken
        // with userId (UUID) as the principal
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            return (UUID) auth.getPrincipal();
        }
        return UUID.fromString(principal.getName());
    }
}
