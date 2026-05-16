package com.ilgiyebo.domain.interaction.entity;

import com.ilgiyebo.common.config.JsonStringListConverter;
import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 유저의 퀴즈 문항.
 * 회원가입 시 AI가 유저 프로필 기반으로 생성하며, 매칭된 상대방이 이 퀴즈를 푼다.
 */
@Entity
@Table(name = "quiz")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class QuizEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "quiz_index", nullable = false)
    private int quizIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Convert(converter = JsonStringListConverter.class)
    @Column(nullable = false, columnDefinition = "JSON")
    private List<String> options;

    @Column(name = "correct_answer", nullable = false)
    private int correctAnswer;
}
