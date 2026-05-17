package com.ilgiyebo.domain.diary.dto;

import jakarta.validation.constraints.NotBlank;

public record DiaryAnswerRequest(
        @NotBlank(message = "답변은 비어있을 수 없습니다")
        String answer
) {}
