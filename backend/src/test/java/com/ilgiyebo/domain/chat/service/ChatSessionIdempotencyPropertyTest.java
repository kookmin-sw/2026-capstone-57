package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.MatchStatus;
import com.ilgiyebo.repository.ChatSessionRepository;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.repository.MatchRepository;
import net.jqwik.api.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Property 3: Session Creation Idempotency
 *
 * For any matchId with existing ACTIVE session, createSession returns existing session
 * and count remains 1.
 *
 * Validates: Requirements 2.5
 */
class ChatSessionIdempotencyPropertyTest {

    private final ChatSessionRepository chatSessionRepository = mock(ChatSessionRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final InteractionRepository interactionRepository = mock(InteractionRepository.class);
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

    private final ChatSessionServiceImpl chatSessionService;

    ChatSessionIdempotencyPropertyTest() {
        chatSessionService = new ChatSessionServiceImpl(
                chatSessionRepository,
                matchRepository,
                interactionRepository,
                redisTemplate,
                messagingTemplate
        );
        ReflectionTestUtils.setField(chatSessionService, "sessionDurationMinutes", 10L);
    }

    /**
     * Property 3: Session Creation Idempotency
     *
     * For any matchId that already has an ACTIVE ChatSessionEntity, calling createSession
     * with that matchId returns the existing session, and no new session is created.
     *
     * Validates: Requirements 2.5
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 3: Session Creation Idempotency")
    void createSessionReturnsExistingActiveSessionWithoutCreatingDuplicate(
            @ForAll("randomMatchIds") UUID matchId) {
        // Arrange: mock MatchRepository to return an ACTIVE match
        MatchEntity match = mock(MatchEntity.class);
        when(match.getStatus()).thenReturn(MatchStatus.ACTIVE);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // Create an existing ACTIVE session for this matchId
        Instant existingStartTime = Instant.now().minus(Duration.ofMinutes(3));
        Instant existingEndTime = existingStartTime.plus(Duration.ofMinutes(10));
        ChatSessionEntity existingSession = ChatSessionEntity.builder()
                .matchId(matchId)
                .startTime(existingStartTime)
                .endTime(existingEndTime)
                .status(ChatSessionStatus.ACTIVE)
                .build();

        // Mock repository to return the existing ACTIVE session
        when(chatSessionRepository.findByMatchIdAndStatus(matchId, ChatSessionStatus.ACTIVE))
                .thenReturn(Optional.of(existingSession));

        // Act: call createSession multiple times
        ChatSessionEntity firstResult = chatSessionService.createSession(matchId);
        ChatSessionEntity secondResult = chatSessionService.createSession(matchId);

        // Assert: both calls return the same existing session
        assertThat(firstResult).isSameAs(existingSession);
        assertThat(secondResult).isSameAs(existingSession);
        assertThat(firstResult.getMatchId()).isEqualTo(matchId);
        assertThat(firstResult.getStatus()).isEqualTo(ChatSessionStatus.ACTIVE);

        // Assert: no new session was saved (save should never be called)
        verify(chatSessionRepository, never()).save(any(ChatSessionEntity.class));
    }

    @Provide
    Arbitrary<UUID> randomMatchIds() {
        return Arbitraries.longs().tuple2().map(t -> new UUID(t.get1(), t.get2()));
    }
}
