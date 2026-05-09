package com.ilgiyebo.dto;

import com.ilgiyebo.domain.NotificationType;
import java.util.Map;

public record NotificationPayloadDto(
    NotificationType type,
    String title,
    String body,
    Map<String, String> data
) {}
