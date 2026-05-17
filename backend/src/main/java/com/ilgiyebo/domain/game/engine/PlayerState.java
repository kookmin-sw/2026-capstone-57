package com.ilgiyebo.domain.game.engine;

import com.ilgiyebo.domain.game.dto.response.PlayerStateDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerState {
    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private boolean onGround;
    private boolean atGoal;

    public PlayerStateDto toDto() {
        return new PlayerStateDto(x, y, velocityX, velocityY, onGround, atGoal);
    }
}
