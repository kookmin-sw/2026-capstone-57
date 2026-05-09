package com.ilgiyebo.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PersonalityType {
    ISTJ("ISTJ"),
    ISFJ("ISFJ"),
    INFJ("INFJ"),
    INTJ("INTJ"),
    ISTP("ISTP"),
    ISFP("ISFP"),
    INFP("INFP"),
    INTP("INTP"),
    ESTP("ESTP"),
    ESFP("ESFP"),
    ENFP("ENFP"),
    ENTP("ENTP"),
    ESTJ("ESTJ"),
    ESFJ("ESFJ"),
    ENFJ("ENFJ"),
    ENTJ("ENTJ");

    private final String label;
}
