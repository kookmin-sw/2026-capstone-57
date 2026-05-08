package com.ilgiyebo.domain.matching.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "`MATCH`")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class MatchEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id", nullable = false)
    private UserEntity userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id", nullable = false)
    private UserEntity userB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_a_id", nullable = false)
    private SlotEntity slotA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_b_id", nullable = false)
    private SlotEntity slotB;

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
