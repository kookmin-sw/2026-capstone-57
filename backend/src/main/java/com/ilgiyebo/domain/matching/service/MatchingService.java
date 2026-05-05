package com.ilgiyebo.service;

import com.ilgiyebo.domain.SlotPriority;
import com.ilgiyebo.dto.RouteOverlapDto;
import com.ilgiyebo.dto.SlotResponseDto;

import java.util.List;
import java.util.UUID;

public interface MatchingService {

    /** 사용자별 슬롯 목록 조회 */
    List<SlotResponseDto> getSlots(UUID userId);

    /** 새 슬롯 해금 (레벨업 보상 연동) */
    SlotResponseDto unlockSlot(UUID userId);

    /** 슬롯 매칭 우선순위 변경 */
    SlotResponseDto updateSlotPriority(UUID userId, UUID slotId, SlotPriority priority);

    /**
     * 두 사용자의 시간표 기반 동선 겹침을 계산한다 (MVP: AI 미사용).
     * 같은 시간대에 같은 건물 또는 인접 건물에 있는 횟수로 점수를 산출한다.
     */
    RouteOverlapDto calculateRouteOverlap(UUID userA, UUID userB);

    /**
     * 두 사용자 간 차단 관계가 있는지 확인한다 (양방향).
     * A가 B를 차단했거나 B가 A를 차단한 경우 true를 반환한다.
     */
    boolean isBlocked(UUID userA, UUID userB);
}
