package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.dto.SessionEndedEvent;
import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import com.ilgiyebo.domain.chat.entity.IcebreakerQuestion;
import com.ilgiyebo.domain.chat.exception.ChatException;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.entity.MatchStatus;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import com.ilgiyebo.domain.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final MatchRepository matchRepository;
    private final InteractionRepository interactionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${chat.session.token-limit:150}")
    private int chatTokenLimit;

    private static final String CACHE_KEY_PREFIX = "chat:session:";

    @Override
    @Transactional
    public ChatSessionEntity createSession(UUID matchId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(ChatException.MATCH_NOT_FOUND::toException);

        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw ChatException.MATCH_NOT_ACTIVE.toException();
        }

        // Idempotency: return existing ACTIVE session if one exists
        return chatSessionRepository.findByMatchIdAndStatus(matchId, ChatSessionStatus.ACTIVE)
                .orElseGet(() -> createNewSession(matchId));
    }

    private ChatSessionEntity createNewSession(UUID matchId) {
        Instant now = Instant.now();

        ChatSessionEntity session = ChatSessionEntity.builder()
                .matchId(matchId)
                .startTime(now)
                .status(ChatSessionStatus.ACTIVE)
                .tokenLimit(chatTokenLimit)
                .usedTokens(0)
                .icebreakerQuestion(IcebreakerQuestion.random().getQuestion())
                .build();

        ChatSessionEntity savedSession = chatSessionRepository.save(session);

        // Update InteractionEntity chatStartTime
        interactionRepository.findByMatchId(matchId).ifPresent(interaction -> {
            interaction.setChatStartTime(savedSession.getStartTime());
            interactionRepository.save(interaction);
        });

        // Cache session in Redis
        cacheSession(savedSession);

        return savedSession;
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSessionEntity getSessionByMatchId(UUID matchId) {
        return chatSessionRepository.findTopByMatchIdOrderByCreatedAtDesc(matchId)
                .orElseThrow(ChatException.SESSION_NOT_FOUND::toException);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSessionEntity getSessionById(UUID sessionId) {
        return chatSessionRepository.findById(sessionId)
                .orElseThrow(ChatException.SESSION_NOT_FOUND::toException);
    }

    @Override
    @Transactional
    public void endSession(UUID sessionId) {
        ChatSessionEntity session = chatSessionRepository.findById(sessionId)
                .orElseThrow(ChatException.SESSION_NOT_FOUND::toException);

        if (session.getStatus() == ChatSessionStatus.ENDED) {
            throw ChatException.SESSION_ALREADY_ENDED.toException();
        }

        Instant endedAt = Instant.now();

        session.setStatus(ChatSessionStatus.ENDED);
        chatSessionRepository.save(session);

        // Update InteractionEntity chatEndTime
        interactionRepository.findByMatchId(session.getMatchId()).ifPresent(interaction -> {
            interaction.setChatEndTime(endedAt);
            interactionRepository.save(interaction);
        });

        // Invalidate Redis cache
        invalidateCache(sessionId);

        SessionEndedEvent event = SessionEndedEvent.builder()
                .sessionId(sessionId)
                .endedAt(endedAt)
                .reason(SessionEndedEvent.TOKEN_LIMIT_REACHED)
                .build();

        messagingTemplate.convertAndSend("/topic/chat/" + sessionId, event);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateParticipant(UUID userId, UUID matchId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(ChatException.MATCH_NOT_FOUND::toException);

        if (!userId.equals(match.getUserA().getId()) && !userId.equals(match.getUserB().getId())) {
            throw ChatException.NOT_PARTICIPANT.toException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateSessionActive(UUID sessionId) {
        // Check Redis cache first for performance optimization
        String cacheKey = CACHE_KEY_PREFIX + sessionId;
        Object cachedStatus = redisTemplate.opsForHash().get(cacheKey, "status");

        if (cachedStatus != null) {
            if (!ChatSessionStatus.ACTIVE.name().equals(cachedStatus.toString())) {
                throw ChatException.SESSION_ALREADY_ENDED.toException();
            }
            return;
        }

        // Fallback to DB on cache miss
        ChatSessionEntity session = chatSessionRepository.findById(sessionId)
                .orElseThrow(ChatException.SESSION_NOT_FOUND::toException);

        if (session.getStatus() != ChatSessionStatus.ACTIVE) {
            throw ChatException.SESSION_ALREADY_ENDED.toException();
        }
    }

    private void cacheSession(ChatSessionEntity session) {
        String key = CACHE_KEY_PREFIX + session.getId();
        redisTemplate.opsForHash().put(key, "status", session.getStatus().name());
        redisTemplate.opsForHash().put(key, "matchId", session.getMatchId().toString());
    }

    private void invalidateCache(UUID sessionId) {
        String key = CACHE_KEY_PREFIX + sessionId;
        redisTemplate.delete(key);
    }
}
