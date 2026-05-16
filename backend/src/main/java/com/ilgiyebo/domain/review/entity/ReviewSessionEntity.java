package com.ilgiyebo.domain.review.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "review_session",
        uniqueConstraints = @UniqueConstraint(columnNames = {"interaction_id", "user_id"}))
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ReviewSessionEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interaction_id", nullable = false)
    private InteractionEntity interaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewMode mode;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewSessionStatus status = ReviewSessionStatus.IN_PROGRESS;
}
