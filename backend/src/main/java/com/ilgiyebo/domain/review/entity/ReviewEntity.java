package com.ilgiyebo.domain.review.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "review")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ReviewEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interaction_id", nullable = false)
    private InteractionEntity interaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewMode mode;

    @Column(nullable = false)
    private int satisfaction;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reflection;

    @Column(name = "want_to_meet_again", nullable = false)
    private boolean wantToMeetAgain;

    @Builder.Default
    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated = false;
}
