package com.gakkomobile.notifications.settings.dto;

import com.gakkomobile.notifications.NotificationMode;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UserNotificationSettingsDTO(
        UUID userId,
        NotificationMode academicGrades,
        NotificationMode academicScheduleChanges,
        NotificationMode academicCancelledClasses,
        NotificationMode financialNewCharges,
        NotificationMode eventsStudent
) {
}
