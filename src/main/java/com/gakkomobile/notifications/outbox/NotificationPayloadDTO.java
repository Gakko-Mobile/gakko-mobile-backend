package com.gakkomobile.notifications.outbox;

import lombok.Builder;

@Builder
public record NotificationPayloadDTO(
        String title,
        String body,
        String category,
        String targetScreen
) {
}
