package com.gakkomobile.notifications.dto;

import java.time.Instant;
import java.util.UUID;

public record InAppNotificationDTO(
        UUID id,
        UUID userId,
        String title,
        String body,
        String category,
        boolean isRead,
        Instant createdAt
) {
}
