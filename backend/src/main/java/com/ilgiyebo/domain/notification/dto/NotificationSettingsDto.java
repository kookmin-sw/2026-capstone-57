package com.ilgiyebo.dto;

public record NotificationSettingsDto(
    boolean matchNotification,
    boolean stageNotification,
    boolean missionReminder,
    boolean plannerReminder,
    boolean levelUpNotification,
    boolean hintQuestionNotification
) {}
