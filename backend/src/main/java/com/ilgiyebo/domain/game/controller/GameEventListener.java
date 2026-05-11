package com.ilgiyebo.domain.game.controller;

import com.ilgiyebo.domain.game.dto.response.RoomStateEvent;
import com.ilgiyebo.domain.game.engine.GameRoom;
import com.ilgiyebo.domain.game.service.GameRoomService;
import com.ilgiyebo.domain.game.service.GameRoomStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Listens for WebSocket session events to track game connections.
 * Handles SUBSCRIBE (join tracking) and DISCONNECT (leave/pause).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventListener {

    private final GameRoomStore roomStore;
    private final GameRoomService gameRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String GAME_TOPIC_PREFIX = "/topic/game/";

    // Track which STOMP session is subscribed to which game session
    private final ConcurrentHashMap<String, UUID> sessionToGameSession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> sessionToUserId = new ConcurrentHashMap<>();

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith(GAME_TOPIC_PREFIX)) {
            return;
        }

        UUID userId = extractUserId(accessor);
        if (userId == null) return;

        String sessionIdStr = destination.substring(GAME_TOPIC_PREFIX.length());
        UUID gameSessionId;
        try {
            gameSessionId = UUID.fromString(sessionIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid game session ID in subscription: {}", sessionIdStr);
            return;
        }

        String stompSessionId = accessor.getSessionId();
        sessionToGameSession.put(stompSessionId, gameSessionId);
        sessionToUserId.put(stompSessionId, userId);

        // Track connection in GameRoom
        roomStore.get(gameSessionId).ifPresent(room -> {
            room.getConnectedUsers().add(userId);
            log.info("사용자 구독 (JOIN): gameSessionId={}, userId={}", gameSessionId, userId);

            // Broadcast ROOM_STATE with current participants
            broadcastRoomState(room);
        });
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String stompSessionId = accessor.getSessionId();

        UUID gameSessionId = sessionToGameSession.remove(stompSessionId);
        UUID userId = sessionToUserId.remove(stompSessionId);

        if (gameSessionId == null || userId == null) {
            return;
        }

        roomStore.get(gameSessionId).ifPresent(room -> {
            room.getConnectedUsers().remove(userId);
            log.info("사용자 연결 해제 (LEAVE): gameSessionId={}, userId={}", gameSessionId, userId);

            // Trigger disconnect handling (pause game if playing)
            gameRoomService.handleDisconnect(gameSessionId, userId);
        });
    }

    private void broadcastRoomState(GameRoom room) {
        Map<String, Boolean> playerStates = room.getReadyState().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        Map.Entry::getValue
                ));

        RoomStateEvent event = new RoomStateEvent("ROOM_STATE", playerStates);
        messagingTemplate.convertAndSend(GAME_TOPIC_PREFIX + room.getSessionId(), event);
    }

    private UUID extractUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object p = auth.getPrincipal();
            if (p instanceof UUID uuid) {
                return uuid;
            }
        }
        if (principal != null) {
            try {
                return UUID.fromString(principal.getName());
            } catch (IllegalArgumentException e) {
                log.warn("Cannot extract userId from principal: {}", principal.getName());
            }
        }
        return null;
    }
}
