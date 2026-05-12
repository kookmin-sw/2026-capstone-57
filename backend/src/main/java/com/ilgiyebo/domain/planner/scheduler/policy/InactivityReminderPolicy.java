package com.ilgiyebo.domain.planner.scheduler.policy;

import com.ilgiyebo.domain.planner.service.PlannerService;
import com.ilgiyebo.domain.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 비활성 사용자 대상 발송 정책 (현재 비활성 정책).
 *
 * <p>3일 이상 source=MANUAL 플래너를 작성하지 않은 사용자에게만 알림을 발송한다.
 * 현재는 전체 발송 정책({@link BroadcastReminderPolicy})이 활성화되어 있어 사용되지 않지만,
 * 향후 리텐션 분석 결과에 따라 비활성 사용자 타겟팅 정책으로 재활성화할 수 있다.</p>
 *
 * <p>활용 시나리오:</p>
 * <ul>
 *   <li>A/B 테스트: 전체 발송 vs 비활성 사용자 대상 발송 비교</li>
 *   <li>알림 피로도 감소: 이미 활발한 사용자에게는 알림을 보내지 않음</li>
 *   <li>리텐션 정책 변경: 이탈 위험 사용자에게만 집중 알림</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class InactivityReminderPolicy implements PlannerReminderPolicy {

    private final PlannerService plannerService;

    @Override
    public boolean shouldSend(UserEntity user) {
        if (user.isSuspended()) {
            return false;
        }
        // TODO: NotificationSetting에서 plannerReminder 동의 여부 확인
        return plannerService.shouldSendInactivityReminder(user.getId());
    }
}
