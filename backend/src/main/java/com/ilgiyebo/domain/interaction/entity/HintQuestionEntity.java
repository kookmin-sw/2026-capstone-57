package com.ilgiyebo.domain.interaction.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "HINT_QUESTION")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class HintQuestionEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_id", nullable = false)
    private UserEntity responder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "quiz_index", nullable = false)
    private int quizIndex;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HintQuestionStatus status = HintQuestionStatus.PENDING;
}