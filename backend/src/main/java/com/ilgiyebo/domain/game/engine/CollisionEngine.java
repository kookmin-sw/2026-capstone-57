package com.ilgiyebo.domain.game.engine;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CollisionEngine {

    // Assumed player dimensions for collision detection
    private static final double PLAYER_WIDTH = 32;
    private static final double PLAYER_HEIGHT = 48;

    /**
     * Resolves floor/platform collisions. Clamps player to platform top and sets onGround.
     */
    public void resolveFloorCollision(PlayerState player, List<Platform> platforms) {
        player.setOnGround(false);

        for (Platform platform : platforms) {
            // Check if player overlaps horizontally with platform
            if (player.getX() + PLAYER_WIDTH > platform.x() &&
                    player.getX() < platform.x() + platform.width()) {

                // Check if player is falling onto the platform (feet at or below platform top)
                double playerBottom = player.getY() + PLAYER_HEIGHT;
                double platformTop = platform.y();

                if (playerBottom >= platformTop &&
                        playerBottom <= platformTop + platform.height() &&
                        player.getVelocityY() >= 0) {

                    // Clamp player to platform top
                    player.setY(platformTop - PLAYER_HEIGHT);
                    player.setVelocityY(0);
                    player.setOnGround(true);
                    return; // Resolve with first matching platform
                }
            }
        }
    }

    /**
     * Updates switches map based on player positions.
     * A switch is pressed when the correct player (or any player if assignedTo is null) stands on it.
     */
    public void checkSwitches(GameState state, List<SwitchArea> switchAreas) {
        Map<String, Boolean> switches = state.getSwitches();

        for (SwitchArea switchArea : switchAreas) {
            boolean pressed = false;

            for (Map.Entry<UUID, PlayerState> entry : state.getPlayers().entrySet()) {
                UUID playerId = entry.getKey();
                PlayerState player = entry.getValue();

                // Check if this player is allowed to activate this switch
                if (switchArea.getAssignedTo() != null && !switchArea.getAssignedTo().equals(playerId)) {
                    continue;
                }

                // Check if player overlaps with switch area
                if (isPlayerInArea(player, switchArea.getX(), switchArea.getY(),
                        switchArea.getWidth(), switchArea.getHeight())) {
                    pressed = true;
                    break;
                }
            }

            switches.put(switchArea.getId(), pressed);
        }
    }

    /**
     * Sets doorOpen when all switches are pressed.
     */
    public void checkDoor(GameState state, List<DoorArea> doors) {
        boolean allSwitchesPressed = !state.getSwitches().isEmpty() &&
                state.getSwitches().values().stream().allMatch(Boolean::booleanValue);
        state.setDoorOpen(allSwitchesPressed);
    }

    /**
     * Sets atGoal for players in the goal area (only when door is open).
     */
    public void checkGoal(GameState state, GoalArea goal) {
        for (PlayerState player : state.getPlayers().values()) {
            if (state.isDoorOpen() && isPlayerInArea(player, goal.x(), goal.y(), goal.width(), goal.height())) {
                player.setAtGoal(true);
            } else {
                player.setAtGoal(false);
            }
        }
    }

    private boolean isPlayerInArea(PlayerState player, double areaX, double areaY,
                                   double areaWidth, double areaHeight) {
        double playerCenterX = player.getX() + PLAYER_WIDTH / 2;
        double playerBottom = player.getY() + PLAYER_HEIGHT;

        return playerCenterX >= areaX &&
                playerCenterX <= areaX + areaWidth &&
                playerBottom >= areaY &&
                playerBottom <= areaY + areaHeight;
    }
}
