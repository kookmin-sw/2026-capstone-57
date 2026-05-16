package com.ilgiyebo.domain.game.service;

import com.ilgiyebo.domain.game.engine.GameRoom;
import com.ilgiyebo.domain.game.engine.GameRoomStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of GameRoomStore backed by ConcurrentHashMap.
 * Thread-safe for concurrent access from STOMP handler threads and GameLoop thread.
 */
@Component
public class InMemoryGameRoomStore implements GameRoomStore {

    private final ConcurrentHashMap<UUID, GameRoom> rooms = new ConcurrentHashMap<>();

    @Override
    public void put(UUID sessionId, GameRoom room) {
        rooms.put(sessionId, room);
    }

    @Override
    public Optional<GameRoom> get(UUID sessionId) {
        return Optional.ofNullable(rooms.get(sessionId));
    }

    @Override
    public void remove(UUID sessionId) {
        rooms.remove(sessionId);
    }

    @Override
    public Collection<GameRoom> getActiveRooms() {
        return rooms.values().stream()
                .filter(room -> {
                    GameRoomStatus status = room.getStatus().get();
                    return status == GameRoomStatus.PLAYING || status == GameRoomStatus.PAUSED;
                })
                .collect(Collectors.toList());
    }
}
