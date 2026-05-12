package com.ilgiyebo.domain.planner.service;

import com.ilgiyebo.domain.planner.dto.PlanEntryRequest;
import com.ilgiyebo.domain.planner.dto.PlanEntryResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PlannerService {

    /** 일정 생성 (source=MANUAL) */
    PlanEntryResponse createPlanEntry(UUID userId, PlanEntryRequest request);

    /** 일정 수정 (MANUAL, SCHEDULE_AUTO 모두 수정 가능) */
    PlanEntryResponse updatePlanEntry(UUID userId, UUID entryId, PlanEntryRequest request);

    /** 일정 삭제 */
    void deletePlanEntry(UUID userId, UUID entryId);

    /** 특정 날짜 일정 목록 조회 (본인만) */
    List<PlanEntryResponse> getPlanEntries(UUID userId, LocalDate date);

    /**
     * 시간표 등록 시 호출
     * 현재 활성 학기를 조회하여 기존 SCHEDULE_AUTO 미래 일정을 삭제하고,
     * 학기 범위 내 PLAN_ENTRY를 자동 생성한다.
     */
    void regenerateScheduleAutoEntries(UUID userId);

    /**
     * 시간표 재등록 전 호출.
     * sourceSchedule FK로 연결된 모든 plan_entry를 삭제하여
     * schedule 삭제 시 FK 제약 위반을 방지한다.
     */
    void deleteScheduleLinkedEntries(UUID userId);

    /**
     * 최근 3일간 source=MANUAL 플래너 미작성 여부를 확인.
     * 향후 비활성 사용자 타겟팅 정책으로 전환 시 재활용 예정.
     */
    boolean shouldSendInactivityReminder(UUID userId);
}
