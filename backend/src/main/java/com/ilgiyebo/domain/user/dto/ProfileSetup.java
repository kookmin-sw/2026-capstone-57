package com.ilgiyebo.dto;

import com.ilgiyebo.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.List;

public record ProfileSetup(
    @NotBlank(message = "닉네임은 필수입니다")
    String nickname,

    @NotBlank(message = "이름은 필수입니다")
    String name,

    @NotBlank(message = "전공은 필수입니다")
    String major,

    @NotNull(message = "생년월일은 필수입니다")
    @Past(message = "생년월일은 과거 날짜여야 합니다")
    LocalDate birthDate,

    @NotNull(message = "성별은 필수입니다")
    Gender gender,

    List<String> hobbies,
    List<String> interests,
    List<String> personalityTypes,
    List<String> idealTypes
) {}
