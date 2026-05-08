package com.ilgiyebo.domain.mission.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.common.config.JsonStringListConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "MISSION")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class MissionEntity extends BaseSchema {

    @Column(name = "match_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID matchId;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String activity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Instant deadline;

    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "confirmed_by", columnDefinition = "JSON")
    private List<String> confirmedBy;

    @Builder.Default
    @Column(nullable = false)
    private boolean extended = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionStatus status = MissionStatus.PENDING;
}
