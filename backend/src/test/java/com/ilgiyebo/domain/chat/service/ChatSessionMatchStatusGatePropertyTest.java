package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.common.exception.BusinessException;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.MatchStatus;
import com.ilgiyebo.repository.ChatSessionRepository;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.repository.MatchRepository;
import net.jqwik.api.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property 4: Match Status Gate
 *
 * For any MatchEntity with status != ACTIVE, createSession throws BusinessException.
 *
 * Validates: Requirements 2.2, 2.3
 */
class ChatSessionMatchStatusGatePropertyTest {

    private final ChatSessionRepository chatSessionRepository = mock(ChatSessionRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final InteractionRepository interactionRepository = mock(InteractionRepository.class);
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

    private final ChatSessionServiceImpl chatSessionService;

    ChatSessionMatchStatusGatePropertyTest() {
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
     * Property 4: Match Status Gate
     *
     * For any MatchEntity with status other than ACTIVE, attempting to create a
     * ChatSessionEntity SHALL result in a BusinessException, and no new session
     * SHALL be persisted.
     *
     * Validates: Requirements 2.2, 2.3
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 4: Match Status Gate")
    void createSessionThrowsBusinessExceptionForNonActiveMatch(
            @ForAll("randomMatchIds") UUID matchId,
            @ForAll("nonActiveStatuses") MatchStatus nonActiveStatus) {
        // Arrange: mock MatchRepository to return a match with non-ACTIVE status
        MatchEntity match = mock(MatchEntity.class);
        when(match.getStatus()).thenReturn(nonActiveStatus);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // Act & Assert: createSession should throw BusinessException
        assertThatThrownBy(() -> chatSessionService.createSession(matchId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("매치가 활성 상태가 아닙니다");

        // Assert: no session was persisted
        verify(chatSessionRepository, never()).save(any());
    }

    @Provide
    Arbitrary<UUID> randomMatchIds() {
        return Arbitraries.longs().tuple2().map(t -> new UUID(t.get1(), t.get2()));
    }

    @Provide
    Arbitrary<MatchStatus> nonActiveStatuses() {
        return Arbitraries.of(MatchStatus.values())
                .filter(status -> status != MatchStatus.ACTIVE);
    }
}
