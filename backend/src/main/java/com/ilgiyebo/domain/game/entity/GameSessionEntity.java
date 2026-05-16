package com.ilgiyebo.domain;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.config.JsonMapConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "GAME_SESSION")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class GameSessionEntity extends BaseSchema {

    @Column(name = "match_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID matchId;

    @Column(name = "game_type", nullable = false, length = 100)
    private String gameType;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "JSON")
    private Map<String, Object> state;

    @Builder.Default
    @Column(name = "intimacy_points", nullable = false)
    private int intimacyPoints = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameSessionStatus status = GameSessionStatus.WAITING;
}
