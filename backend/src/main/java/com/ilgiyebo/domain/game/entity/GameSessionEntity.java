package com.ilgiyebo.domain.game.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.common.config.JsonMapConverter;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Entity
@Table(name = "GAME_SESSION")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class GameSessionEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

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
