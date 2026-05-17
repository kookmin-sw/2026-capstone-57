package com.ilgiyebo.domain.diary.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "diary_session", indexes = {
        @Index(name = "idx_diary_session_user_date_status", columnList = "user_id, target_date, status"),
        @Index(name = "idx_diary_session_user_status", columnList = "user_id, status")
})
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class DiarySessionEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DiarySessionStatus status = DiarySessionStatus.IN_PROGRESS;

    @Column(name = "generated_content", columnDefinition = "TEXT")
    private String generatedContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_emotion", length = 20)
    private EmotionTag suggestedEmotion;

    @Builder.Default
    @Column(name = "max_turns", nullable = false)
    private int maxTurns = 5;

    @Builder.Default
    @Column(name = "current_turn", nullable = false)
    private int currentTurn = 0;

    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("turnNumber ASC")
    private List<DiaryConversationTurnEntity> conversationTurns = new ArrayList<>();
}
