package com.ilgiyebo.domain.exp.dto;

import com.ilgiyebo.domain.exp.entity.ExpActivity;

public record ExpGrantDto(
    ExpActivity activity,
    int amount,
    int bonusAmount,
    int newTotal
) {}
