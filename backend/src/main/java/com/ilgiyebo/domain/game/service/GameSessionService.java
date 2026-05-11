package com.ilgiyebo.domain.game.service;

import com.ilgiyebo.domain.game.dto.response.GameSessionResponse;
import com.ilgiyebo.domain.game.entity.GameFailReason;

import java.util.UUID;

public interface GameSessionService {

    GameSessionResponse createSession(UUID matchId, UUID requesterId);

    GameSessionResponse getSession(UUID gameSessionId, UUID requesterId);

    void startGame(UUID gameSessionId);

    void completeGame(UUID gameSessionId, int score, long clearTimeMs, String finalStateJson);

    void failGame(UUID gameSessionId, GameFailReason reason, int partialScore, String finalStateJson);

    void expireSession(UUID gameSessionId);
}
