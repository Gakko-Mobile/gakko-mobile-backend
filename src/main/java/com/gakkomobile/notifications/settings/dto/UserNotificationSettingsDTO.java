package com.gakkomobile.notifications.settings.dto;

import com.gakkomobile.notifications.NotificationMode;

import java.util.UUID;

public record UserNotificationSettingsDTO(
        UUID userId,
        NotificationMode academicGrades,
        NotificationMode academicScheduleChanges,
        NotificationMode academicCancelledClasses,
        NotificationMode financialNewCharges,
        NotificationMode eventsStudent
) {
}
