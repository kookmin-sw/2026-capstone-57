package com.ilgiyebo.domain.matching.entity;

/**
 * 슬롯의 매칭 우선순위.
 * AI에게 유저 프로필 정보와 함께 전달되어 매칭 점수 계산 시 가중치로 활용된다.
 */
public enum SlotPriority {
    HOBBY,       // 취미 기반 매칭 우선
    INTEREST,    // 관심사 기반 매칭 우선
    IDEAL_TYPE   // 이상형 기반 매칭 우선
}
