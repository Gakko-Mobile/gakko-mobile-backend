package com.gakkomobile.notifications.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record InAppNotificationResponseDTO(
        UUID id,
        UUID userId,
        String title,
        String body,
        String category,
        boolean isRead,
        Instant createdAt
) {
}
