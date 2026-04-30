package com.ilgiyebo.domain;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.config.JsonStringListConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "`USER`")
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

    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "personality_type", columnDefinition = "JSON")
    private List<String> personalityTypes;

    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "ideal_type_preferences", columnDefinition = "JSON")
    private List<String> idealTypes;

    @Builder.Default
    @Column(name = "total_exp", nullable = false)
    private int totalExp = 0;

    @Builder.Default
    @Column(name = "current_level", nullable = false)
    private int currentLevel = 1;

    @Builder.Default
    @Column(name = "is_suspended", nullable = false)
    private boolean isSuspended = false;
}
