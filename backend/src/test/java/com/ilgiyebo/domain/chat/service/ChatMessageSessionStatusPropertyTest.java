package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.common.exception.BusinessException;
import com.ilgiyebo.domain.chat.entity.ChatMessageEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import com.ilgiyebo.repository.ChatMessageRepository;
import net.jqwik.api.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property 6: Session Status Determines Message Acceptance
 *
 * Messages are accepted if and only if the session status is ACTIVE.
 * ENDED sessions reject messages with a BusinessException (SESSION_ALREADY_ENDED).
 *
 * Validates: Requirements 4.3, 4.4
 */
class ChatMessageSessionStatusPropertyTest {

    private ChatMessageRepository chatMessageRepository;
    private ChatSessionService chatSessionService;
    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> redisTemplate;
    @SuppressWarnings("unchecked")
    private HashOperations<String, Object, Object> hashOperations;

    private ChatMessageServiceImpl chatMessageService;

    private void setupMocks() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        chatSessionService = mock(ChatSessionService.class);
        redisTemplate = mock(RedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        chatMessageService = new ChatMessageServiceImpl(
                chatMessageRepository,
                chatSessionService,
                redisTemplate
        );
    }

    /**
     * Property 6a: ACTIVE sessions accept messages.
     *
     * For any ACTIVE session with a valid participant and non-blank content,
     * saveMessage succeeds and returns a persisted entity.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 6: Session Status Determines Message Acceptance")
    void activeSessionAcceptsMessages(
            @ForAll("randomUUIDs") UUID sessionId,
            @ForAll("randomUUIDs") UUID senderId,
            @ForAll("randomUUIDs") UUID matchId,
            @ForAll("validContent") String content) {

        setupMocks();

        // Arrange: Redis cache indicates ACTIVE
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("chat:session:" + sessionId, "status"))
                .thenReturn(ChatSessionStatus.ACTIVE.name());

        ChatSessionEntity session = ChatSessionEntity.builder()
                .matchId(matchId)
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(600))
                .status(ChatSessionStatus.ACTIVE)
                .build();
        when(chatSessionService.getSessionById(sessionId)).thenReturn(session);
        doNothing().when(chatSessionService).validateParticipant(senderId, matchId);

        when(chatMessageRepository.save(any(ChatMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ChatMessageEntity result = chatMessageService.saveMessage(sessionId, senderId, content);

        // Assert: message is accepted and persisted with correct fields
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        assertThat(result.getSenderId()).isEqualTo(senderId);
        assertThat(result.getContent()).isEqualTo(content);
        verify(chatMessageRepository).save(any(ChatMessageEntity.class));
    }

    /**
     * Property 6b: ENDED sessions reject messages (via Redis cache).
     *
     * For any ENDED session (status cached in Redis), saveMessage throws
     * BusinessException with SESSION_ALREADY_ENDED message.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 6: Session Status Determines Message Acceptance")
    void endedSessionRejectsMessagesViaCachedStatus(
            @ForAll("randomUUIDs") UUID sessionId,
            @ForAll("randomUUIDs") UUID senderId,
            @ForAll("validContent") String content) {

        setupMocks();

        // Arrange: Redis cache indicates ENDED
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("chat:session:" + sessionId, "status"))
                .thenReturn(ChatSessionStatus.ENDED.name());

        // Act & Assert: message is rejected
        assertThatThrownBy(() -> chatMessageService.saveMessage(sessionId, senderId, content))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 종료된 채팅 세션입니다");

        // Verify no message was persisted
        verify(chatMessageRepository, never()).save(any());
    }

    /**
     * Property 6c: ENDED sessions reject messages (via DB fallback).
     *
     * When Redis cache has no entry, the service falls back to DB.
     * If the session status in DB is ENDED, saveMessage throws BusinessException.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 6: Session Status Determines Message Acceptance")
    void endedSessionRejectsMessagesViaDbFallback(
            @ForAll("randomUUIDs") UUID sessionId,
            @ForAll("randomUUIDs") UUID senderId,
            @ForAll("randomUUIDs") UUID matchId,
            @ForAll("validContent") String content) {

        setupMocks();

        // Arrange: Redis cache miss (returns null)
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("chat:session:" + sessionId, "status"))
                .thenReturn(null);

        // DB returns session with ENDED status
        ChatSessionEntity endedSession = ChatSessionEntity.builder()
                .matchId(matchId)
                .startTime(Instant.now().minusSeconds(600))
                .endTime(Instant.now())
                .status(ChatSessionStatus.ENDED)
                .build();
        when(chatSessionService.getSessionById(sessionId)).thenReturn(endedSession);

        // Act & Assert: message is rejected
        assertThatThrownBy(() -> chatMessageService.saveMessage(sessionId, senderId, content))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 종료된 채팅 세션입니다");

        // Verify no message was persisted
        verify(chatMessageRepository, never()).save(any());
    }

    @Provide
    Arbitrary<UUID> randomUUIDs() {
        return Arbitraries.longs().tuple2().map(t -> new UUID(t.get1(), t.get2()));
    }

    @Provide
    Arbitrary<String> validContent() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(500)
                .filter(s -> !s.isBlank());
    }
}
