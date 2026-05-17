package com.ilgiyebo.domain.user.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.common.config.JsonStringListConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "`user`")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class UserEntity extends BaseSchema {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(length = 100)
    private String name;

    @Column(nullable = false)
    private String university;

    @Column(length = 255)
    private String major;

    @Column(name = "student_id", length = 50)
    private String studentId;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "JSON")
    private List<String> hobbies;

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "JSON")
    private List<String> interests;

    @Enumerated(EnumType.STRING)
    @Column(name = "personality_type", length = 4)
    private PersonalityType personalityType;

    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "ideal_type_preferences", columnDefinition = "JSON")
    private List<String> idealTypes;

    @Builder.Default
    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "ai_inferred_hobbies", columnDefinition = "JSON")
    private List<String> aiInferredHobbies = new java.util.ArrayList<>();

    @Builder.Default
    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "ai_inferred_interests", columnDefinition = "JSON")
    private List<String> aiInferredInterests = new java.util.ArrayList<>();

    @Builder.Default
    @Column(name = "total_exp", nullable = false)
    private int totalExp = 0;

    @Builder.Default
    @Column(name = "current_level", nullable = false)
    private int currentLevel = 1;

    @Builder.Default
    @Column(name = "is_suspended", nullable = false)
    private boolean isSuspended = false;

    /**
     * 퀴즈 생성 요청 시각.
     * 일정 시간이 지나도 퀴즈가 없으면 재요청 트리거로 사용한다.
     */
    @Column(name = "quiz_requested_at")
    private Instant quizRequestedAt;
}
