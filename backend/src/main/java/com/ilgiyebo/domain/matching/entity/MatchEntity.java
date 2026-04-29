package com.ilgiyebo.domain;

import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "`MATCH`")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class MatchEntity extends BaseSchema {

    @Column(name = "user_a_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userAId;

    @Column(name = "user_b_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userBId;

    @Column(name = "slot_a_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID slotAId;

    @Column(name = "slot_b_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID slotBId;

    @Builder.Default
    @Column(name = "is_quick_match", nullable = false)
    private boolean isQuickMatch = false;

    @Column(name = "cycle_start_date", nullable = false)
    private LocalDate cycleStartDate;

    @Column(name = "cycle_end_date", nullable = false)
    private LocalDate cycleEndDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.ACTIVE;
}
