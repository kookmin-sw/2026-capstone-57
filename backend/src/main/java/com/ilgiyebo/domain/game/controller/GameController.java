package com.ilgiyebo.domain.game.controller;

import com.ilgiyebo.common.exception.BusinessException;
import com.ilgiyebo.domain.game.dto.request.GameActionMessage;
import com.ilgiyebo.domain.game.dto.request.PositionUpdateData;
import com.ilgiyebo.domain.game.dto.response.GameErrorEvent;
import com.ilgiyebo.domain.game.service.GameRoomService;
import com.ilgiyebo.domain.game.service.RelayService;
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
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameRoomService gameRoomService;
    private final RelayService relayService;

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
            case "POSITION_UPDATE" -> {
                PositionUpdateData positionData = extractPositionData(message.payload());
                if (positionData == null) {
                    log.debug("POSITION_UPDATE with invalid payload ignored: sessionId={}, userId={}", sessionId, userId);
                    return;
                }
                relayService.relayPosition(sessionId, userId, positionData);
            }
            case "COIN_COLLECTED" -> {
                String coinId = extractTargetId(message.payload());
                if (coinId == null) {
                    log.debug("COIN_COLLECTED with null targetId ignored: sessionId={}, userId={}", sessionId, userId);
                    return;
                }
                relayService.handleCoinCollected(sessionId, userId, coinId);
            }
            case "SWITCH_PRESSED" -> {
                String switchId = extractTargetId(message.payload());
                if (switchId == null) {
                    log.debug("SWITCH_PRESSED with null targetId ignored: sessionId={}, userId={}", sessionId, userId);
                    return;
                }
                relayService.handleSwitchEvent(sessionId, userId, switchId, true);
            }
            case "SWITCH_RELEASED" -> {
                String switchId = extractTargetId(message.payload());
                if (switchId == null) {
                    log.debug("SWITCH_RELEASED with null targetId ignored: sessionId={}, userId={}", sessionId, userId);
                    return;
                }
                relayService.handleSwitchEvent(sessionId, userId, switchId, false);
            }
            case "CLEAR_REQUEST" -> {
                relayService.handleClearRequest(sessionId, userId);
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

    private PositionUpdateData extractPositionData(Map<String, Object> payload) {
        if (payload == null) return null;
        try {
            double x = toDouble(payload.get("x"));
            double y = toDouble(payload.get("y"));
            double velocityX = toDouble(payload.get("velocityX"));
            double velocityY = toDouble(payload.get("velocityY"));
            String animation = (String) payload.getOrDefault("animation", "idle");
            boolean flipX = Boolean.TRUE.equals(payload.get("flipX"));
            long timestamp = toLong(payload.get("timestamp"));
            return new PositionUpdateData(x, y, velocityX, velocityY, animation, flipX, timestamp);
        } catch (Exception e) {
            log.debug("Failed to parse position data: {}", e.getMessage());
            return null;
        }
    }

    private String extractTargetId(Map<String, Object> payload) {
        if (payload == null) return null;
        Object targetId = payload.get("targetId");
        return targetId != null ? targetId.toString() : null;
    }

    private double toDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return 0.0;
    }

    private long toLong(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        return 0L;
    }
}
