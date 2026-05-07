package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import com.ilgiyebo.domain.InteractionEntity;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.MatchStatus;
import com.ilgiyebo.repository.ChatSessionRepository;
import com.ilgiyebo.repository.InteractionRepository;
import com.ilgiyebo.repository.MatchRepository;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
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
 * Property 9: InteractionEntity Synchronization
 *
 * On session create: InteractionEntity.chatStartTime = session.startTime
 * On session end: InteractionEntity.chatEndTime = actual end time
 *
 * Validates: Requirements 2.4, 6.2
 */
class ChatSessionInteractionSyncPropertyTest {

    private ChatSessionRepository chatSessionRepository;
    private MatchRepository matchRepository;
    private InteractionRepository interactionRepository;
    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> redisTemplate;
    private SimpMessagingTemplate messagingTemplate;
    private ChatSessionServiceImpl chatSessionService;

    private void setupFreshMocks() {
        chatSessionRepository = mock(ChatSessionRepository.class);
        matchRepository = mock(MatchRepository.class);
        interactionRepository = mock(InteractionRepository.class);
        redisTemplate = mock(RedisTemplate.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
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
     * Property 9a: On session create, InteractionEntity.chatStartTime equals session.startTime.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 9: InteractionEntity Synchronization (create)")
    void onSessionCreateInteractionChatStartTimeEqualsSessionStartTime(
            @ForAll("randomMatchIds") UUID matchId) {
        setupFreshMocks();

        // Arrange: mock MatchRepository to return an ACTIVE match
        MatchEntity match = mock(MatchEntity.class);
        when(match.getStatus()).thenReturn(MatchStatus.ACTIVE);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // No existing ACTIVE session
        when(chatSessionRepository.findByMatchIdAndStatus(matchId, ChatSessionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // Mock save to return the entity passed to it
        when(chatSessionRepository.save(any(ChatSessionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock InteractionEntity that exists for this match
        InteractionEntity interaction = InteractionEntity.builder()
                .matchId(matchId)
                .build();
        when(interactionRepository.findByMatchId(matchId)).thenReturn(Optional.of(interaction));
        when(interactionRepository.save(any(InteractionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock RedisTemplate operations
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        // Act
        ChatSessionEntity session = chatSessionService.createSession(matchId);

        // Assert: InteractionEntity.chatStartTime == session.startTime
        ArgumentCaptor<InteractionEntity> interactionCaptor = ArgumentCaptor.forClass(InteractionEntity.class);
        verify(interactionRepository).save(interactionCaptor.capture());

        InteractionEntity savedInteraction = interactionCaptor.getValue();
        assertThat(savedInteraction.getChatStartTime()).isNotNull();
        assertThat(savedInteraction.getChatStartTime()).isEqualTo(session.getStartTime());
    }

    /**
     * Property 9b: On session end, InteractionEntity.chatEndTime equals the actual end time.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 9: InteractionEntity Synchronization (end)")
    void onSessionEndInteractionChatEndTimeEqualsActualEndTime(
            @ForAll("randomMatchIds") UUID matchId,
            @ForAll("randomSessionIds") UUID sessionId) {
        setupFreshMocks();

        // Arrange: create an ACTIVE session
        Instant startTime = Instant.now().minus(Duration.ofMinutes(5));
        Instant originalEndTime = startTime.plus(Duration.ofMinutes(10));

        ChatSessionEntity session = ChatSessionEntity.builder()
                .matchId(matchId)
                .startTime(startTime)
                .endTime(originalEndTime)
                .status(ChatSessionStatus.ACTIVE)
                .build();
        // Set the ID via reflection since BaseSchema likely manages it
        ReflectionTestUtils.setField(session, "id", sessionId);

        when(chatSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(chatSessionRepository.save(any(ChatSessionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock InteractionEntity that exists for this match
        InteractionEntity interaction = InteractionEntity.builder()
                .matchId(matchId)
                .chatStartTime(startTime)
                .build();
        when(interactionRepository.findByMatchId(matchId)).thenReturn(Optional.of(interaction));
        when(interactionRepository.save(any(InteractionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Mock RedisTemplate delete
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act
        chatSessionService.endSession(sessionId);

        // Assert: InteractionEntity.chatEndTime == session's actual endTime (set during endSession)
        ArgumentCaptor<InteractionEntity> interactionCaptor = ArgumentCaptor.forClass(InteractionEntity.class);
        verify(interactionRepository).save(interactionCaptor.capture());

        InteractionEntity savedInteraction = interactionCaptor.getValue();
        assertThat(savedInteraction.getChatEndTime()).isNotNull();
        // The actual end time set in endSession is Instant.now(), which is also set on the session
        assertThat(savedInteraction.getChatEndTime()).isEqualTo(session.getEndTime());
    }

    @Provide
    Arbitrary<UUID> randomMatchIds() {
        return Arbitraries.longs().tuple2().map(t -> new UUID(t.get1(), t.get2()));
    }

    @Provide
    Arbitrary<UUID> randomSessionIds() {
        return Arbitraries.longs().tuple2().map(t -> new UUID(t.get1(), t.get2()));
    }
}
