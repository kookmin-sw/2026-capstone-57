package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.entity.MatchStatus;
import com.ilgiyebo.domain.chat.repository.ChatSessionRepository;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import net.jqwik.api.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Property 1: Session Timing Invariant
 *
 * For any newly created ChatSessionEntity, endTime = startTime + 10 minutes.
 *
 * Validates: Requirements 2.1
 */
class ChatSessionTimingPropertyTest {

    private final ChatSessionRepository chatSessionRepository = mock(ChatSessionRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final InteractionRepository interactionRepository = mock(InteractionRepository.class);
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

    private final ChatSessionServiceImpl chatSessionService;

    ChatSessionTimingPropertyTest() {
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
     * Property 1: Session Timing Invariant
     *
     * For any newly created ChatSessionEntity, endTime = startTime + 10 minutes.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 1: Session Timing Invariant")
    void sessionEndTimeEqualsStartTimePlusTenMinutes(@ForAll("randomMatchIds") UUID matchId) {
        // Arrange: mock MatchRepository to return an ACTIVE match
        MatchEntity match = mock(MatchEntity.class);
        when(match.getStatus()).thenReturn(MatchStatus.ACTIVE);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // Mock no existing ACTIVE session (forces creation of new session)
        when(chatSessionRepository.findByMatchIdAndStatus(matchId, ChatSessionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // Mock save to return the entity passed to it
        when(chatSessionRepository.save(any(ChatSessionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock InteractionRepository to return empty
        when(interactionRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

        // Mock RedisTemplate operations
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        // Act
        ChatSessionEntity session = chatSessionService.createSession(matchId);

        // Assert: endTime must equal startTime + exactly 10 minutes
        assertThat(session.getStartTime()).isNotNull();
        assertThat(session.getEndTime()).isNotNull();
        assertThat(session.getEndTime())
                .isEqualTo(session.getStartTime().plus(Duration.ofMinutes(10)));
    }

    @Provide
    Arbitrary<UUID> randomMatchIds() {
        return Arbitraries.longs().tuple2().map(t -> new UUID(t.get1(), t.get2()));
    }
}
