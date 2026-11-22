package com.gakkomobile.notifications.outbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationPayloadDTO(
        String body,
        String category,
        String targetScreen,
        String messageKey
) {
}
