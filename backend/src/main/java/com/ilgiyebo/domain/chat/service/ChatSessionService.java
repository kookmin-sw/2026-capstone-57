package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;

import java.util.UUID;

public interface ChatSessionService {

    ChatSessionEntity createSession(UUID matchId);

    ChatSessionEntity getSessionByMatchId(UUID matchId);

    ChatSessionEntity getSessionById(UUID sessionId);

    void endSession(UUID sessionId);

    void endExpiredSessions();

    void validateParticipant(UUID userId, UUID matchId);
}
