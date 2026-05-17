package com.ilgiyebo.domain.planner.scheduler.policy;

import com.ilgiyebo.domain.user.entity.UserEntity;

/**
 * 플래너 작성 알림 발송 정책 인터페이스.
 *
 * <p>알림 대상 사용자를 결정하는 전략을 추상화한다.
 * 비즈니스 정책 변경(전체 발송, 비활성 사용자 대상, A/B 테스트 등)에
 * 유연하게 대응할 수 있도록 Strategy Pattern으로 설계되었다.</p>
 *
 * <p>구현체 예시:</p>
 * <ul>
 *   <li>{@link BroadcastReminderPolicy} - 알림 동의 유저 전체에게 발송 (현재 활성 정책)</li>
 *   <li>{@link InactivityReminderPolicy} - 3일 이상 미작성 유저에게만 발송 (비활성 정책, 재사용 가능)</li>
 * </ul>
 */
public interface PlannerReminderPolicy {

    /**
     * 해당 사용자에게 플래너 작성 알림을 발송해야 하는지 판단한다.
     *
     * @param user 대상 사용자
     * @return 알림 발송 여부
     */
    boolean shouldSend(UserEntity user);
}
