package com.ilgiyebo.domain.notification.dto;

public record NotificationSettingsDto(
    boolean matchNotification,
    boolean stageNotification,
    boolean missionReminder,
    boolean plannerReminder,
    boolean levelUpNotification,
    boolean hintQuestionNotification
) {}
