package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatMessageEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import com.ilgiyebo.domain.chat.exception.ChatException;
import com.ilgiyebo.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionService chatSessionService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "chat:session:";

    @Override
    @Transactional
    public ChatMessageEntity saveMessage(UUID sessionId, UUID senderId, String content) {
        // Validate content is not blank
        if (content == null || content.isBlank()) {
            throw ChatException.INVALID_MESSAGE.toException();
        }

        // Validate session is ACTIVE (check Redis cache first, fallback to DB)
        validateSessionActive(sessionId);

        // Validate sender is a participant of the session's match
        ChatSessionEntity session = chatSessionService.getSessionById(sessionId);
        chatSessionService.validateParticipant(senderId, session.getMatchId());

        // Persist message
        ChatMessageEntity message = ChatMessageEntity.builder()
                .sessionId(sessionId)
                .senderId(senderId)
                .content(content)
                .build();

        return chatMessageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageEntity> getMessagesBySessionId(UUID sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    private void validateSessionActive(UUID sessionId) {
        // Check Redis cache first
        String cacheKey = CACHE_KEY_PREFIX + sessionId;
        Object cachedStatus = redisTemplate.opsForHash().get(cacheKey, "status");

        if (cachedStatus != null) {
            if (!ChatSessionStatus.ACTIVE.name().equals(cachedStatus.toString())) {
                throw ChatException.SESSION_ALREADY_ENDED.toException();
            }
            return;
        }

        // Fallback to DB
        ChatSessionEntity session = chatSessionService.getSessionById(sessionId);
        if (session.getStatus() != ChatSessionStatus.ACTIVE) {
            throw ChatException.SESSION_ALREADY_ENDED.toException();
        }
    }
}
