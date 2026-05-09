package com.ilgiyebo.domain;

import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "NOTIFICATION_SETTING")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class NotificationSettingEntity extends BaseSchema {

    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

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
