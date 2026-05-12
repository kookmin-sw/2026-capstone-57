package com.ilgiyebo.domain.planner.scheduler;

import com.ilgiyebo.domain.planner.scheduler.policy.PlannerReminderPolicy;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 플래너 작성 알림 스케줄러.
 *
 * 매일 오전 9시에 {@link PlannerReminderPolicy}에 따라 대상 사용자를 선별하고
 * 플래너 작성 알림을 전송한다.
 *
 * 현재 활성 정책: {@code BroadcastReminderPolicy} (전체 발송).
 * 정책 변경 시 주입되는 구현체만 교체하면 된다.
 */
@Slf4j
@Component
public class PlannerReminderScheduler {

    private final UserRepository userRepository;
    private final PlannerReminderPolicy reminderPolicy;

    public PlannerReminderScheduler(
            UserRepository userRepository,
            @Qualifier("broadcastReminderPolicy") PlannerReminderPolicy reminderPolicy) {
        this.userRepository = userRepository;
        this.reminderPolicy = reminderPolicy;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyPlannerReminder() {
        log.info("플래너 작성 알림 스케줄러 실행 (정책: {})", reminderPolicy.getClass().getSimpleName());

        long sentCount = userRepository.findAll().stream()
                .filter(reminderPolicy::shouldSend)
                .peek(user -> {
                    // TODO: NotificationService 구현 후 실제 알림 전송
                    log.debug("플래너 작성 알림 발송: userId={}", user.getId());
                })
                .count();

        log.info("플래너 작성 알림 발송 완료: 대상 {}명", sentCount);
    }
}
