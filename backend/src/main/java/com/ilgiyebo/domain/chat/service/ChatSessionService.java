package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;

import java.util.UUID;

public interface ChatSessionService {

    ChatSessionEntity createSession(UUID matchId);

    ChatSessionEntity getSessionByMatchId(UUID matchId);

    ChatSessionEntity getSessionById(UUID sessionId);

    void endSession(UUID sessionId);

    void validateParticipant(UUID userId, UUID matchId);

    /**
     * Validates that the session is ACTIVE.
     * Checks Redis cache first for performance, falls back to DB on cache miss.
     *
     * @param sessionId the session ID to validate
     * @throws com.ilgiyebo.common.exception.BusinessException if session is not ACTIVE
     */
    void validateSessionActive(UUID sessionId);
}
