package com.ilgiyebo.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PersonalityType {
    INTROVERTED("내향적인"),
    EXTROVERTED("외향적인"),
    CALM("차분한"),
    ENERGETIC("활발한"),
    HUMOROUS("유머러스한"),
    SERIOUS("진지한"),
    CREATIVE("창의적인"),
    LOGICAL("논리적인"),
    EMPATHETIC("공감 잘하는"),
    INDEPENDENT("독립적인"),
    ADVENTUROUS("모험적인"),
    CAUTIOUS("신중한"),
    OPTIMISTIC("낙관적인"),
    REALISTIC("현실적인"),
    SPONTANEOUS("즉흥적인"),
    PLANNED("계획적인"),
    CARING("배려심 깊은"),
    COMPETITIVE("승부욕 강한"),
    EASYGOING("느긋한"),
    PASSIONATE("열정적인");

    private final String label;
}
