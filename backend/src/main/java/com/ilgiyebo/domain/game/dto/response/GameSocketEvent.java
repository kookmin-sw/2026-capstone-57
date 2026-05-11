package com.ilgiyebo.domain.game.dto.response;

public sealed interface GameSocketEvent permits
        RoomStateEvent, GameStartedEvent, StateUpdateEvent,
        GameClearedEvent, GameOverEvent, PlayerDisconnectedEvent,
        PlayerReconnectedEvent, RestartRequestedEvent, GameErrorEvent {
    String type();
}
