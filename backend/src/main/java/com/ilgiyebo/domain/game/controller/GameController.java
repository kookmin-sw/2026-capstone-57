package com.ilgiyebo.domain.game.controller;

import com.ilgiyebo.common.exception.BusinessException;
import com.ilgiyebo.domain.game.dto.request.GameActionMessage;
import com.ilgiyebo.domain.game.dto.response.GameErrorEvent;
import com.ilgiyebo.domain.game.service.GameRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameRoomService gameRoomService;

    @MessageMapping("/game/{sessionId}/action")
    public void handleGameAction(
            @DestinationVariable UUID sessionId,
            @Valid GameActionMessage message,
            Principal principal) {

        UUID userId = extractUserId(principal);

        switch (message.type()) {
            case "READY" -> {
                gameRoomService.registerParticipant(sessionId, userId);
                gameRoomService.setReady(sessionId, userId);
            }
            case "PLAYER_INPUT" -> {
                if (message.input() == null) {
                    log.debug("PLAYER_INPUT with null input ignored: sessionId={}, userId={}", sessionId, userId);
                    return;
                }
                gameRoomService.bufferInput(sessionId, userId, message.input());
            }
            case "RESTART_REQUEST" -> {
                gameRoomService.requestRestart(sessionId, userId);
            }
            default -> log.warn("Unknown game action type: {}, sessionId={}, userId={}",
                    message.type(), sessionId, userId);
        }
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public GameErrorEvent handleBusinessException(BusinessException ex) {
        log.warn("Game action error: {}", ex.getMessage());
        return new GameErrorEvent("GAME_ERROR", "BUSINESS_ERROR", ex.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public GameErrorEvent handleException(Exception ex) {
        log.error("Unexpected game error", ex);
        return new GameErrorEvent("GAME_ERROR", "INTERNAL_ERROR", "게임 처리 중 오류가 발생했습니다");
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            return (UUID) auth.getPrincipal();
        }
        return UUID.fromString(principal.getName());
    }
}
