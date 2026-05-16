package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.common.exception.BusinessException;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.chat.repository.ChatSessionRepository;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import net.jqwik.api.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Property 2: Participant Authorization
 *
 * User granted access iff userId matches userAId or userBId of associated MatchEntity.
 *
 * Validates: Requirements 1.6, 3.2, 4.6, 5.2, 7.3
 */
class ChatSessionParticipantAuthorizationPropertyTest {

    private final ChatSessionRepository chatSessionRepository = mock(ChatSessionRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final InteractionRepository interactionRepository = mock(InteractionRepository.class);
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

    private final ChatSessionServiceImpl chatSessionService;

    ChatSessionParticipantAuthorizationPropertyTest() {
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
     * Property 2a: Participant Authorization - Access Granted
     *
     * For any userId that equals userAId or userBId of the associated MatchEntity,
     * validateParticipant SHALL NOT throw an exception.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 2: Participant Authorization")
    void participantWithMatchingUserIdIsGrantedAccess(
            @ForAll("randomUUIDs") UUID userAId,
            @ForAll("randomUUIDs") UUID userBId,
            @ForAll("randomUUIDs") UUID matchId,
            @ForAll boolean useUserA) {

        // Arrange: mock MatchRepository to return a match with userAId and userBId
        MatchEntity match = mock(MatchEntity.class);
        UserEntity userAEntity = mock(UserEntity.class);
        UserEntity userBEntity = mock(UserEntity.class);
        when(userAEntity.getId()).thenReturn(userAId);
        when(userBEntity.getId()).thenReturn(userBId);
        when(match.getUserA()).thenReturn(userAEntity);
        when(match.getUserB()).thenReturn(userBEntity);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // Select either userA or userB as the requesting user
        UUID requestingUserId = useUserA ? userAId : userBId;

        // Act & Assert: validateParticipant should NOT throw
        assertThatCode(() -> chatSessionService.validateParticipant(requestingUserId, matchId))
                .doesNotThrowAnyException();
    }

    /**
     * Property 2b: Participant Authorization - Access Denied
     *
     * For any userId that does NOT equal userAId and does NOT equal userBId
     * of the associated MatchEntity, validateParticipant SHALL throw a
     * BusinessException with FORBIDDEN status.
     */
    @Property(tries = 100)
    @Tag("Feature: chat-realtime-messaging, Property 2: Participant Authorization")
    void nonParticipantUserIsDeniedAccess(
            @ForAll("randomUUIDs") UUID userAId,
            @ForAll("randomUUIDs") UUID userBId,
            @ForAll("randomUUIDs") UUID nonParticipantId,
            @ForAll("randomUUIDs") UUID matchId) {

        // Ensure nonParticipantId is different from both userAId and userBId
        Assume.that(!nonParticipantId.equals(userAId) && !nonParticipantId.equals(userBId));

        // Arrange: mock MatchRepository to return a match with userAId and userBId
        MatchEntity match = mock(MatchEntity.class);
        UserEntity userAEntity = mock(UserEntity.class);
        UserEntity userBEntity = mock(UserEntity.class);
        when(userAEntity.getId()).thenReturn(userAId);
        when(userBEntity.getId()).thenReturn(userBId);
        when(match.getUserA()).thenReturn(userAEntity);
        when(match.getUserB()).thenReturn(userBEntity);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // Act & Assert: validateParticipant should throw BusinessException with FORBIDDEN
        assertThatThrownBy(() -> chatSessionService.validateParticipant(nonParticipantId, matchId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("채팅 참여자가 아닙니다")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Provide
    Arbitrary<UUID> randomUUIDs() {
        return Arbitraries.longs().tuple2().map(t -> new UUID(t.get1(), t.get2()));
    }
}
