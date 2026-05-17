package com.ilgiyebo.domain.game.engine;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SwitchArea {
    private String id;
    private double x;
    private double y;
    private double width;
    private double height;
    private UUID assignedTo; // nullable - null means any player can activate
}
