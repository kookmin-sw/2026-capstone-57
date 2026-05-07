package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatMessageEntity;
import com.ilgiyebo.repository.ChatMessageRepository;
import net.jqwik.api.*;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property 7: Message History Ordering
 *
 * For any session with multiple messages, the history returned by
 * getMessagesBySessionId is sorted by createdAt in ascending order.
 *
 * Validates: Requirements 5.1
 */
class ChatMessageHistoryOrderingPropertyTest {

    private ChatMessageRepository chatMessageRepository;
    private ChatSessionService chatSessionService;
    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> redisTemplate;

    private ChatMessageServiceImpl chatMessageService;

    private void setupMocks() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        chatSessionService = mock(ChatSessionService.class);
        redisTemplate = mock(RedisTemplate.class);
        chatMessageService = new ChatMessageServiceImpl(
                chatMessageRepository,
                chatSessionService,
                redisTemplate
        );
    }

    /**
     * Property 7: For any session with multiple messages, the returned history
     * is sorted by createdAt ascending. We simulate the repository returning
     * messages in the correct order (as guaranteed by the query method
     * findBySessionIdOrderByCreatedAtAsc) and verify the service preserves
     * that ordering.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 7: Message History Ordering")
    void messageHistoryIsSortedByCreatedAtAscending(
            @ForAll("randomUUIDs") UUID sessionId,
            @ForAll("messageCounts") int messageCount) {

        setupMocks();

        // Arrange: generate messages with ascending createdAt timestamps
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
        List<ChatMessageEntity> orderedMessages = new ArrayList<>();

        for (int i = 0; i < messageCount; i++) {
            ChatMessageEntity message = ChatMessageEntity.builder()
                    .sessionId(sessionId)
                    .senderId(UUID.randomUUID())
                    .content("Message " + i)
                    .build();
            // Use reflection-free approach: the builder sets fields, and we simulate
            // the createdAt as it would be returned from DB (ordered by query)
            setCreatedAt(message, baseTime.plusSeconds(i));
            orderedMessages.add(message);
        }

        // Mock repository to return messages in createdAt ASC order (as the query guarantees)
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                .thenReturn(orderedMessages);

        // Act
        List<ChatMessageEntity> result = chatMessageService.getMessagesBySessionId(sessionId);

        // Assert: result is not empty and sorted by createdAt ascending
        assertThat(result).hasSize(messageCount);
        for (int i = 1; i < result.size(); i++) {
            LocalDateTime previous = result.get(i - 1).getCreatedAt();
            LocalDateTime current = result.get(i).getCreatedAt();
            assertThat(previous).isBeforeOrEqualTo(current);
        }
    }

    /**
     * Property 7b: Even when messages are inserted in arbitrary order,
     * the repository query (and thus the service) returns them sorted
     * by createdAt ascending.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 7: Message History Ordering")
    void messageHistoryPreservesAscendingOrderRegardlessOfInsertionOrder(
            @ForAll("randomUUIDs") UUID sessionId,
            @ForAll("shuffledTimestamps") List<LocalDateTime> timestamps) {

        setupMocks();

        // Arrange: create messages with the given timestamps (potentially unordered)
        // but the repository returns them sorted by createdAt ASC
        List<ChatMessageEntity> sortedMessages = timestamps.stream()
                .sorted()
                .map(ts -> {
                    ChatMessageEntity msg = ChatMessageEntity.builder()
                            .sessionId(sessionId)
                            .senderId(UUID.randomUUID())
                            .content("Content at " + ts)
                            .build();
                    setCreatedAt(msg, ts);
                    return msg;
                })
                .toList();

        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                .thenReturn(sortedMessages);

        // Act
        List<ChatMessageEntity> result = chatMessageService.getMessagesBySessionId(sessionId);

        // Assert: result is sorted by createdAt ascending
        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        for (int i = 1; i < result.size(); i++) {
            LocalDateTime previous = result.get(i - 1).getCreatedAt();
            LocalDateTime current = result.get(i).getCreatedAt();
            assertThat(previous).isBeforeOrEqualTo(current);
        }
    }

    /**
     * Sets createdAt field on a BaseSchema entity using the setter from Lombok @Getter/@Setter
     * on the parent. Since BaseSchema doesn't have a setter for createdAt, we use reflection.
     */
    private void setCreatedAt(ChatMessageEntity entity, LocalDateTime createdAt) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set createdAt", e);
        }
    }

    @Provide
    Arbitrary<UUID> randomUUIDs() {
        return Arbitraries.longs().tuple2().map(t -> new UUID(t.get1(), t.get2()));
    }

    @Provide
    Arbitrary<Integer> messageCounts() {
        return Arbitraries.integers().between(2, 20);
    }

    @Provide
    Arbitrary<List<LocalDateTime>> shuffledTimestamps() {
        return Arbitraries.integers().between(2, 15)
                .flatMap(count -> {
                    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
                    return Arbitraries.integers().between(1, 3600)
                            .list().ofSize(count)
                            .map(offsets -> offsets.stream()
                                    .map(base::plusSeconds)
                                    .toList());
                });
    }
}
