package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.dto.SessionEndedEvent;
import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import com.ilgiyebo.domain.chat.exception.ChatException;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.MatchStatus;
import com.ilgiyebo.repository.MatchRepository;
import com.ilgiyebo.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final MatchRepository matchRepository;
    private final InteractionRepository interactionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${chat.session.duration-minutes:10}")
    private long sessionDurationMinutes;

    private static final String CACHE_KEY_PREFIX = "chat:session:";

    @Override
    @Transactional
    public ChatSessionEntity createSession(UUID matchId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(ChatException.SESSION_NOT_FOUND::toException);

        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw ChatException.MATCH_NOT_ACTIVE.toException();
        }

        // Idempotency: return existing ACTIVE session if one exists
        return chatSessionRepository.findByMatchIdAndStatus(matchId, ChatSessionStatus.ACTIVE)
                .orElseGet(() -> createNewSession(matchId));
    }

    private ChatSessionEntity createNewSession(UUID matchId) {
        Instant now = Instant.now();
        Instant endTime = now.plus(Duration.ofMinutes(sessionDurationMinutes));

        ChatSessionEntity session = ChatSessionEntity.builder()
                .matchId(matchId)
                .startTime(now)
                .endTime(endTime)
                .status(ChatSessionStatus.ACTIVE)
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
        return chatSessionRepository.findByMatchId(matchId)
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

        Instant actualEndTime = Instant.now();
        Instant originalEndTime = session.getEndTime();

        session.setStatus(ChatSessionStatus.ENDED);
        session.setEndTime(actualEndTime);
        chatSessionRepository.save(session);

        // Update InteractionEntity chatEndTime
        interactionRepository.findByMatchId(session.getMatchId()).ifPresent(interaction -> {
            interaction.setChatEndTime(actualEndTime);
            interactionRepository.save(interaction);
        });

        // Invalidate Redis cache
        invalidateCache(sessionId);

        // Determine reason: MANUAL if ended before original endTime, TIME_EXPIRED otherwise
        String reason = actualEndTime.isBefore(originalEndTime)
                ? SessionEndedEvent.MANUAL
                : SessionEndedEvent.TIME_EXPIRED;

        SessionEndedEvent event = SessionEndedEvent.builder()
                .sessionId(sessionId)
                .endedAt(actualEndTime)
                .reason(reason)
                .build();

        messagingTemplate.convertAndSend("/topic/chat/" + sessionId, event);
    }

    @Override
    @Transactional
    public void endExpiredSessions() {
        List<ChatSessionEntity> expiredSessions = chatSessionRepository
                .findAllByStatusAndEndTimeBefore(ChatSessionStatus.ACTIVE, Instant.now());

        for (ChatSessionEntity session : expiredSessions) {
            try {
                endSession(session.getId());
            } catch (Exception e) {
                log.error("Failed to end expired session: {}", session.getId(), e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateParticipant(UUID userId, UUID matchId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(ChatException.SESSION_NOT_FOUND::toException);

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
        long ttlSeconds = Duration.between(Instant.now(), session.getEndTime()).getSeconds();

        if (ttlSeconds > 0) {
            redisTemplate.opsForHash().put(key, "status", session.getStatus().name());
            redisTemplate.opsForHash().put(key, "endTime", session.getEndTime().toString());
            redisTemplate.opsForHash().put(key, "matchId", session.getMatchId().toString());
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
    }

    private void invalidateCache(UUID sessionId) {
        String key = CACHE_KEY_PREFIX + sessionId;
        redisTemplate.delete(key);
    }
}
