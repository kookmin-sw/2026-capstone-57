package com.ilgiyebo.domain.review.entity;

import com.ilgiyebo.common.config.ConversationHistoryConverter;
import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.review.service.ReviewAiClient.ConversationTurn;
import com.ilgiyebo.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

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

    /** AI 멀티턴 대화 히스토리 (JSON) */
    @Builder.Default
    @Convert(converter = ConversationHistoryConverter.class)
    @Column(name = "conversation_history", columnDefinition = "JSON")
    private List<ConversationTurn> conversationHistory = new ArrayList<>();

    /** AI가 제공한 다음 질문 (아직 답변되지 않은 현재 질문) */
    @Column(name = "current_question", columnDefinition = "TEXT")
    private String currentQuestion;

    /** AI가 설정한 최대 턴 수 */
    @Builder.Default
    @Column(name = "max_turns")
    private int maxTurns = 5;
}
