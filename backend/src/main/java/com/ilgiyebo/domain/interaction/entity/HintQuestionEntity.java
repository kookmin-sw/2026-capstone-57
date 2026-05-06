package com.ilgiyebo.domain.interaction.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "HINT_QUESTION")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class HintQuestionEntity extends BaseSchema {

    @Column(name = "match_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID matchId;

    @Column(name = "sender_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID senderId;

    @Column(name = "responder_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID responderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HintQuestionStatus status = HintQuestionStatus.PENDING;

    @Column(name = "answered_at")
    private Instant answeredAt;
}
