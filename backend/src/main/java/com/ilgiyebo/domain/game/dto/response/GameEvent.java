package com.ilgiyebo.domain.game.dto.response;

public sealed interface GameEvent permits
        RoomStateEvent, GameStartedEvent, StateUpdateEvent,
        GameClearedEvent, GameOverEvent, PlayerDisconnectedEvent,
        PlayerReconnectedEvent, RestartRequestedEvent, GameErrorEvent {
    String type();
}
