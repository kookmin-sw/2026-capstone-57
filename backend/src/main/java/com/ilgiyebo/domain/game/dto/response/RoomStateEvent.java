package com.ilgiyebo.domain.game.dto.response;

import java.util.Map;

public record RoomStateEvent(
        String type,
        Map<String, Boolean> players
) implements GameSocketEvent {}
