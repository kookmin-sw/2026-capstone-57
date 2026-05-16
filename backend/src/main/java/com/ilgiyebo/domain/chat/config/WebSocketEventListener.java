package com.ilgiyebo.domain.chat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    // sessionId -> subscribed destination (세션별 구독 토픽 추적)
    private final ConcurrentHashMap<String, String> sessionDestinations = new ConcurrentHashMap<>();
    // sessionId -> userId
    private final ConcurrentHashMap<String, UUID> sessionUsers = new ConcurrentHashMap<>();

    private static final String TOPIC_PREFIX = "/topic/chat/";

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        if (destination == null || !destination.startsWith(TOPIC_PREFIX)) {
            return;
        }

        // /topic/chat/{sessionId} 형태만 처리, /topic/chat/{sessionId}/end 등 하위 경로는 무시
        String chatSessionId = destination.substring(TOPIC_PREFIX.length());
        if (chatSessionId.contains("/")) {
            return;
        }

        UUID userId = extractUserId(accessor);
        if (userId == null) return;

        sessionDestinations.put(sessionId, destination);
        sessionUsers.put(sessionId, userId);

        log.info("유저 입장: userId={}, sessionId={}", userId, chatSessionId);

        messagingTemplate.convertAndSend(destination, Map.of(
                "type", "JOIN",
                "userId", userId.toString(),
                "timestamp", Instant.now().toString()
        ));
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        sendLeaveEvent(sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        sendLeaveEvent(sessionId);
    }

    private void sendLeaveEvent(String sessionId) {
        String destination = sessionDestinations.remove(sessionId);
        UUID userId = sessionUsers.remove(sessionId);

        if (destination == null || userId == null) return;

        String chatSessionId = destination.substring(TOPIC_PREFIX.length());
        log.info("유저 퇴장: userId={}, sessionId={}", userId, chatSessionId);

        messagingTemplate.convertAndSend(destination, Map.of(
                "type", "LEAVE",
                "userId", userId.toString(),
                "timestamp", Instant.now().toString()
        ));
    }

    private UUID extractUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            Object principal = accessor.getUser();
            if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
                return (UUID) auth.getPrincipal();
            }
        }
        return null;
    }
}
