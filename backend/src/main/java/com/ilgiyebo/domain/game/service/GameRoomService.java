package com.ilgiyebo.domain.game.service;

import com.ilgiyebo.domain.game.dto.request.PlayerInputData;

import java.util.UUID;

public interface GameRoomService {

    void registerParticipant(UUID sessionId, UUID userId);

    void setReady(UUID sessionId, UUID userId);

    void bufferInput(UUID sessionId, UUID userId, PlayerInputData input);

    void handleDisconnect(UUID sessionId, UUID userId);

    void handleReconnect(UUID sessionId, UUID userId);

    void requestRestart(UUID sessionId, UUID userId);
}
