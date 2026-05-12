package com.ilgiyebo.domain.planner.scheduler.policy;

import com.ilgiyebo.domain.user.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * 전체 발송 정책 (현재 활성 정책).
 *
 * <p>정지되지 않은 모든 사용자에게 플래너 작성 알림을 발송한다.
 * 알림 수신 동의 여부는 NotificationService 구현 후 추가 필터링 예정.</p>
 */
@Component
public class BroadcastReminderPolicy implements PlannerReminderPolicy {

    @Override
    public boolean shouldSend(UserEntity user) {
        // 정지된 계정은 제외
        if (user.isSuspended()) {
            return false;
        }
        // TODO: NotificationSetting에서 plannerReminder 동의 여부 확인
        return true;
    }
}
