package com.ilgiyebo.domain.notification.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "NOTIFICATION_SETTING")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class NotificationSettingEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Builder.Default
    @Column(name = "match_notification", nullable = false)
    private boolean matchNotification = true;

    @Builder.Default
    @Column(name = "stage_notification", nullable = false)
    private boolean stageNotification = true;

    @Builder.Default
    @Column(name = "mission_reminder", nullable = false)
    private boolean missionReminder = true;

    @Builder.Default
    @Column(name = "planner_reminder", nullable = false)
    private boolean plannerReminder = true;

    @Builder.Default
    @Column(name = "level_up_notification", nullable = false)
    private boolean levelUpNotification = true;

    @Builder.Default
    @Column(name = "hint_question_notification", nullable = false)
    private boolean hintQuestionNotification = true;
}
