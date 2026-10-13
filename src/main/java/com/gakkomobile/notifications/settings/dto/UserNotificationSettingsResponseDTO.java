package com.gakkomobile.notifications.settings.dto;

import com.gakkomobile.notifications.NotificationMode;

public record UserNotificationSettingsResponseDTO(
        NotificationMode academicGrades,
        NotificationMode academicScheduleChanges,
        NotificationMode academicCancelledClasses,
        NotificationMode financialNewCharges,
        NotificationMode eventsStudent
) {
}
