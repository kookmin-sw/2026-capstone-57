package com.ilgiyebo.domain.safety.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "REPORT")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ReportEntity extends BaseSchema {

    @Column(name = "reporter_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID reporterId;

    @Column(name = "target_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID targetId;

    @Column(name = "match_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID matchId;

    @Column(nullable = false)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING;
}
