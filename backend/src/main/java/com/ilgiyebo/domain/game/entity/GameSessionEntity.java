package com.ilgiyebo.domain.game.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_session", indexes = {
        @Index(name = "idx_game_session_match_id", columnList = "match_id"),
        @Index(name = "idx_game_session_status", columnList = "status"),
        @Index(name = "idx_game_session_match_status", columnList = "match_id, status")
})
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class GameSessionEntity extends BaseSchema {

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", insertable = false, updatable = false)
    private MatchEntity match;

    @Column(name = "game_type", nullable = false, length = 50)
    private String gameType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameSessionStatus status = GameSessionStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_reason", length = 20)
    private GameFailReason failReason;

    @Builder.Default
    @Column(name = "score")
    private Integer score = 0;

    @Column(name = "clear_time_ms")
    private Long clearTimeMs;

    @Builder.Default
    @Column(name = "intimacy_points")
    private Integer intimacyPoints = 0;

    @Column(name = "final_state", columnDefinition = "JSON")
    private String finalState;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
