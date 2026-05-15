package com.ilgiyebo.domain.review.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerReviewQuestionRequest(
    @NotBlank String answer
) {}
