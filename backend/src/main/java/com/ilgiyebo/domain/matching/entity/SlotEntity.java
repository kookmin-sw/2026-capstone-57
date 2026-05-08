package com.ilgiyebo.domain.matching.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "SLOT")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class SlotEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * 매칭 우선순위: 이 슬롯에서 매칭할 때 어떤 기준을 우선시할지.
     * HOBBY(취미), INTEREST(관심사), IDEAL_TYPE(이상형)
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private SlotPriority priority = SlotPriority.HOBBY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_match_id")
    private MatchEntity currentMatch;

    @Builder.Default
    @Column(name = "is_quick_match", nullable = false)
    private boolean isQuickMatch = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status = SlotStatus.EMPTY;
}
