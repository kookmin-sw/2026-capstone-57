package com.ilgiyebo.domain.game.service;

import com.ilgiyebo.domain.game.engine.GameRoom;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstraction for GameRoom storage.
 * MVP uses in-memory implementation; can be replaced with Redis-backed store for scaling.
 */
public interface GameRoomStore {

    void put(UUID sessionId, GameRoom room);

    Optional<GameRoom> get(UUID sessionId);

    void remove(UUID sessionId);

    Collection<GameRoom> getActiveRooms();
}
