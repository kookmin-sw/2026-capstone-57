package com.ilgiyebo.domain.game.engine;

import com.ilgiyebo.domain.game.dto.request.PlayerInputData;
import org.springframework.stereotype.Component;

@Component
public class PhysicsEngine {

    // Movement speed in pixels per millisecond
    public static final double MOVE_SPEED = 0.3;

    // Jump velocity (negative = upward in screen coordinates)
    public static final double JUMP_VELOCITY = -0.6;

    // Gravity acceleration in pixels per millisecond squared
    public static final double GRAVITY = 0.002;

    // Maximum fall speed in pixels per millisecond
    public static final double MAX_FALL_SPEED = 0.8;

    /**
     * Applies player input to set horizontal velocity and handle jumping.
     */
    public void applyInput(PlayerState player, PlayerInputData input, double deltaMs) {
        // Horizontal movement
        if (input.left() && !input.right()) {
            player.setVelocityX(-MOVE_SPEED);
        } else if (input.right() && !input.left()) {
            player.setVelocityX(MOVE_SPEED);
        } else {
            player.setVelocityX(0);
        }

        // Jump (only when on ground)
        if (input.jump() && player.isOnGround()) {
            player.setVelocityY(JUMP_VELOCITY);
            player.setOnGround(false);
        }
    }

    /**
     * Applies gravity to the player's vertical velocity.
     */
    public void applyGravity(PlayerState player, double deltaMs) {
        if (!player.isOnGround()) {
            double newVelocityY = player.getVelocityY() + GRAVITY * deltaMs;
            player.setVelocityY(Math.min(newVelocityY, MAX_FALL_SPEED));
        }
    }

    /**
     * Updates player position based on current velocity.
     */
    public void applyMovement(PlayerState player, double deltaMs) {
        player.setX(player.getX() + player.getVelocityX() * deltaMs);
        player.setY(player.getY() + player.getVelocityY() * deltaMs);
    }
}
