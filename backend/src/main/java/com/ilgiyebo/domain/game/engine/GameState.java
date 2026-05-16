package com.ilgiyebo.domain.game.engine;

import com.ilgiyebo.domain.game.dto.response.GameStateSnapshot;
import com.ilgiyebo.domain.game.dto.response.PlayerStateDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    private Map<UUID, PlayerState> players;
    private Map<String, Boolean> switches;
    private boolean doorOpen;
    private double remainingTimeMs;
    private long elapsedTimeMs;
    private int cooperationCount;
    private int score;

    /**
     * Creates an immutable snapshot of the current game state for broadcast.
     * Thread-safe: copies all mutable state at the point of invocation.
     */
    public GameStateSnapshot toSnapshot() {
        Map<String, PlayerStateDto> playerDtos = players.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toDto()
                ));

        return new GameStateSnapshot(
                playerDtos,
                Map.copyOf(switches),
                doorOpen,
                remainingTimeMs,
                elapsedTimeMs,
                cooperationCount,
                score
        );
    }
}
