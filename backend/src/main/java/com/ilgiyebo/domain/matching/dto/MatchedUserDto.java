package com.ilgiyebo.dto;

import java.util.UUID;

/**
 * 매칭된 상대방의 요약 정보.
 * 슬롯 목록 조회 시 currentMatchId가 존재하면 함께 반환된다.
 */
public record MatchedUserDto(
    UUID userId,
    String nickname
) {}
