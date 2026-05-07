package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatMessageEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import com.ilgiyebo.repository.ChatMessageRepository;
import net.jqwik.api.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Property 5: Message Persistence Round-Trip
 *
 * For any valid message saved to an ACTIVE session, querying by sessionId returns
 * a message with the same sessionId, senderId, and content.
 *
 * Validates: Requirements 4.2
 */
class ChatMessagePersistenceRoundTripPropertyTest {

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatSessionService chatSessionService = mock(ChatSessionService.class);
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);

    private final ChatMessageServiceImpl chatMessageService;

    ChatMessagePersistenceRoundTripPropertyTest() {
        chatMessageService = new ChatMessageServiceImpl(
                chatMessageRepository,
                chatSessionService,
                redisTemplate
        );
    }

    /**
     * Property 5: Message Persistence Round-Trip
     *
     * For any valid (non-blank) content, random senderId, and random sessionId
     * belonging to an ACTIVE session where the sender is a participant,
     * saving the message and then querying by sessionId returns a message
     * with the same sessionId, senderId, and content.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 5: Message Persistence Round-Trip")
    void savedMessageIsRetrievableWithSameFields(
            @ForAll("randomUUIDs") UUID sessionId,
            @ForAll("randomUUIDs") UUID senderId,
            @ForAll("randomUUIDs") UUID matchId,
            @ForAll("validContent") String content) {

        // Arrange: mock Redis cache to indicate session is ACTIVE
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("chat:session:" + sessionId, "status"))
                .thenReturn(ChatSessionStatus.ACTIVE.name());

        // Mock session retrieval for participant validation
        ChatSessionEntity session = ChatSessionEntity.builder()
                .matchId(matchId)
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(600))
                .status(ChatSessionStatus.ACTIVE)
                .build();
        when(chatSessionService.getSessionById(sessionId)).thenReturn(session);

        // Mock validateParticipant to succeed (sender is a valid participant)
        doNothing().when(chatSessionService).validateParticipant(senderId, matchId);

        // Mock repository save to return the entity with same fields
        when(chatMessageRepository.save(any(ChatMessageEntity.class)))
                .thenAnswer(invocation -> {
                    ChatMessageEntity saved = invocation.getArgument(0);
                    // Simulate JPA persisting and returning the entity
                    return saved;
                });

        // Mock repository query to return the saved message
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                .thenAnswer(invocation -> {
                    // Build the entity as it would be returned from DB
                    ChatMessageEntity persisted = ChatMessageEntity.builder()
                            .sessionId(sessionId)
                            .senderId(senderId)
                            .content(content)
                            .build();
                    return List.of(persisted);
                });

        // Act: save the message
        ChatMessageEntity savedMessage = chatMessageService.saveMessage(sessionId, senderId, content);

        // Assert: saved message has correct fields
        assertThat(savedMessage.getSessionId()).isEqualTo(sessionId);
        assertThat(savedMessage.getSenderId()).isEqualTo(senderId);
        assertThat(savedMessage.getContent()).isEqualTo(content);

        // Act: retrieve messages by sessionId
        List<ChatMessageEntity> retrievedMessages = chatMessageService.getMessagesBySessionId(sessionId);

        // Assert: round-trip preserves sessionId, senderId, content
        assertThat(retrievedMessages).isNotEmpty();
        ChatMessageEntity retrieved = retrievedMessages.get(0);
        assertThat(retrieved.getSessionId()).isEqualTo(sessionId);
        assertThat(retrieved.getSenderId()).isEqualTo(senderId);
        assertThat(retrieved.getContent()).isEqualTo(content);
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
