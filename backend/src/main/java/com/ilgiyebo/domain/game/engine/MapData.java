package com.ilgiyebo.domain.game.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapData {
    private double width;
    private double height;
    private List<Platform> platforms;
    private List<SwitchArea> switches;
    private List<DoorArea> doors;
    private GoalArea goal;
    private Position spawnA;
    private Position spawnB;
    private double timeLimitMs;
}
