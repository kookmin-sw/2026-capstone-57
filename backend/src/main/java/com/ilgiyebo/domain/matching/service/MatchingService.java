package com.ilgiyebo.service;

import com.ilgiyebo.domain.SlotPriority;
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
}
