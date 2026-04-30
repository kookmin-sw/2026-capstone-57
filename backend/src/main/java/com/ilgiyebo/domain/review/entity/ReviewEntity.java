package com.ilgiyebo.domain;

import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "REVIEW")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ReviewEntity extends BaseSchema {

    @Column(name = "interaction_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID interactionId;

    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

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
