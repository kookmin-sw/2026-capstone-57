package com.ilgiyebo.domain.exp.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "exp_history")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ExpHistoryEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpActivity activity;

    @Column(nullable = false)
    private int amount;

    @Builder.Default
    @Column(name = "bonus_amount", nullable = false)
    private int bonusAmount = 0;
}
