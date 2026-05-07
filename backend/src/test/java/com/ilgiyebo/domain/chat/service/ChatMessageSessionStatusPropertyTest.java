package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.common.exception.BusinessException;
import com.ilgiyebo.domain.chat.entity.ChatMessageEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import com.ilgiyebo.domain.chat.exception.ChatException;
import com.ilgiyebo.repository.ChatMessageRepository;
import net.jqwik.api.*;

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

    private ChatMessageServiceImpl chatMessageService;

    private void setupMocks() {
        chatMessageRepository = mock(ChatMessageRepository.class);
        chatSessionService = mock(ChatSessionService.class);
        chatMessageService = new ChatMessageServiceImpl(
                chatMessageRepository,
                chatSessionService
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

        // Arrange: validateSessionActive passes (session is ACTIVE)
        doNothing().when(chatSessionService).validateSessionActive(sessionId);

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
     * Property 6b: ENDED sessions reject messages.
     *
     * For any ENDED session, validateSessionActive throws BusinessException
     * with SESSION_ALREADY_ENDED message, and saveMessage propagates it.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 6: Session Status Determines Message Acceptance")
    void endedSessionRejectsMessages(
            @ForAll("randomUUIDs") UUID sessionId,
            @ForAll("randomUUIDs") UUID senderId,
            @ForAll("validContent") String content) {

        setupMocks();

        // Arrange: validateSessionActive throws (session is ENDED)
        doThrow(ChatException.SESSION_ALREADY_ENDED.toException())
                .when(chatSessionService).validateSessionActive(sessionId);

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
