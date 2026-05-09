package com.ilgiyebo.domain.notification.dto;

import com.ilgiyebo.domain.notification.entity.NotificationType;
import java.util.Map;

public record NotificationPayloadDto(
    NotificationType type,
    String title,
    String body,
    Map<String, String> data
) {}
