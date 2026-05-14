package com.ilgiyebo.domain.mission.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.common.config.JsonStringListConverter;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "mission")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class MissionEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionStatus status = MissionStatus.PENDING;

    /**
     * AI가 선택한 서브 노드 ID (추적용).
     * RAG 기반 미션 생성 시 AI 서버가 선택한 장소의 ChromaDB 노드 ID.
     */
    @Column(name = "selected_node_id")
    private String selectedNodeId;
}
