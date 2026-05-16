package com.ilgiyebo.domain.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerReviewQuestionRequest(
    @NotBlank @Size(max = 5000) String answer
) {}
