package com.ilgiyebo.domain.auth.dto;

import com.ilgiyebo.domain.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.List;

public record SignupRequest(
    @NotBlank String verificationId,
    @NotBlank String password,
    @NotBlank String nickname,
    @NotBlank String name,
    @NotBlank String major,
    String studentId,

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
