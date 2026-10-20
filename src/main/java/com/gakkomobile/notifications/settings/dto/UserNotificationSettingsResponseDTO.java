package com.gakkomobile.notifications.settings.dto;

import com.gakkomobile.notifications.NotificationMode;
import lombok.Builder;

@Builder
public record UserNotificationSettingsResponseDTO(
        NotificationMode academicGrades,
        NotificationMode academicScheduleChanges,
        NotificationMode academicCancelledClasses,
        NotificationMode financialNewCharges,
        NotificationMode eventsStudent
) {
}
