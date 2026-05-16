package com.ilgiyebo.domain.interaction.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 매칭에서 유저가 상대방의 퀴즈를 풀었을 때의 답안 기록.
 * user = 답안을 제출한 유저, quiz = 상대방의 퀴즈 문항.
 */
@Entity
@Table(name = "quiz_answer")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class QuizAnswerEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    /** 답안을 제출한 유저 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** 풀어야 할 퀴즈 문항 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizEntity quiz;

    @Column(name = "user_answer", nullable = false)
    private int userAnswer;
}
