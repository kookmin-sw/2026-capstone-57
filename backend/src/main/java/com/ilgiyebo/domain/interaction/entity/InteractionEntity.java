package com.ilgiyebo.domain.interaction.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.config.JsonStringListConverter;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionDto;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "INTERACTION")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class InteractionEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @Builder.Default
    @Column(name = "current_stage", nullable = false)
    private int currentStage = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "stage_status", nullable = false)
    private StageStatus stageStatus = StageStatus.IN_PROGRESS;

    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "quiz_completed_by", columnDefinition = "JSON")
    private List<String> quizCompletedBy;

    @Convert(converter = JsonQuizDataConverter.class)
    @Column(name = "quiz_data", columnDefinition = "JSON")
    private List<QuizQuestionDto> quizData;

    @Column(name = "chat_start_time")
    private Instant chatStartTime;

    @Column(name = "chat_end_time")
    private Instant chatEndTime;

    @Column(name = "game_type", length = 100)
    private String gameType;

    @Builder.Default
    @Column(name = "game_completed", nullable = false)
    private boolean gameCompleted = false;

    @Column(name = "mission_id", columnDefinition = "BINARY(16)")
    private UUID missionId;

    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "mission_confirmed_by", columnDefinition = "JSON")
    private List<String> missionConfirmedBy;

    @Builder.Default
    @Column(name = "mission_extended", nullable = false)
    private boolean missionExtended = false;

    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "review_completed_by", columnDefinition = "JSON")
    private List<String> reviewCompletedBy;

    @Column(name = "termination_reason")
    private String terminationReason;
}