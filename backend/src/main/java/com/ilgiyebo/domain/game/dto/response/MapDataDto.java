package com.ilgiyebo.domain.game.dto.response;

import com.ilgiyebo.domain.game.engine.*;

import java.util.List;

public record MapDataDto(
        double width,
        double height,
        List<PlatformDto> platforms,
        List<SwitchDto> switches,
        List<DoorDto> doors,
        GoalDto goal,
        PositionDto spawnA,
        PositionDto spawnB,
        double timeLimitMs
) {
    public static MapDataDto from(MapData mapData) {
        return new MapDataDto(
                mapData.getWidth(),
                mapData.getHeight(),
                mapData.getPlatforms().stream().map(p -> new PlatformDto(p.x(), p.y(), p.width(), p.height())).toList(),
                mapData.getSwitches().stream().map(s -> new SwitchDto(s.getId(), s.getX(), s.getY(), s.getWidth(), s.getHeight())).toList(),
                mapData.getDoors().stream().map(d -> new DoorDto(d.x(), d.y(), d.width(), d.height())).toList(),
                new GoalDto(mapData.getGoal().x(), mapData.getGoal().y(), mapData.getGoal().width(), mapData.getGoal().height()),
                new PositionDto(mapData.getSpawnA().x(), mapData.getSpawnA().y()),
                new PositionDto(mapData.getSpawnB().x(), mapData.getSpawnB().y()),
                mapData.getTimeLimitMs()
        );
    }

    public record PlatformDto(double x, double y, double width, double height) {}
    public record SwitchDto(String id, double x, double y, double width, double height) {}
    public record DoorDto(double x, double y, double width, double height) {}
    public record GoalDto(double x, double y, double width, double height) {}
    public record PositionDto(double x, double y) {}
}
