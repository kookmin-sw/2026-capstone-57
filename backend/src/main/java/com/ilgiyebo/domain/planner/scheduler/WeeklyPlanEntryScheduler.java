package com.ilgiyebo.domain.planner.scheduler;

import com.ilgiyebo.domain.planner.dto.ScheduleAutoGenerateResult;
import com.ilgiyebo.domain.planner.service.PlannerService;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주간 PLAN_ENTRY 배치 생성 스케줄러.
 * 매주 일요일 23:00에 실행하여, 다음 주의 SCHEDULE_AUTO PLAN_ENTRY를 사전 생성한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyPlanEntryScheduler {

    private final PlannerService plannerService;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 23 * * SUN")
    public void generateNextWeekEntries() {
        log.info("주간 PLAN_ENTRY 배치 생성 시작");

        userRepository.findAll().stream()
                .filter(user -> !user.isSuspended())
                .forEach(user -> {
                    try {
                        ScheduleAutoGenerateResult result = plannerService.generateNextWeekEntries(user.getId());
                        if (result.createdCount() > 0) {
                            log.debug("다음 주 PLAN_ENTRY 생성: userId={}, created={}, skipped={}",
                                    user.getId(), result.createdCount(), result.skippedCount());
                        }
                    } catch (Exception e) {
                        log.warn("다음 주 PLAN_ENTRY 생성 실패: userId={}", user.getId(), e);
                    }
                });

        log.info("주간 PLAN_ENTRY 배치 생성 완료");
    }
}
