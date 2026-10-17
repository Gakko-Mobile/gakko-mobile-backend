package com.gakkomobile.notifications.settings.dto;

import com.gakkomobile.notifications.NotificationMode;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingsRequestDTO(
        @NotNull
        NotificationMode academicGrades,
        @NotNull
        NotificationMode academicScheduleChanges,
        @NotNull
        NotificationMode academicCancelledClasses,
        @NotNull
        NotificationMode financialNewCharges,
        @NotNull
        NotificationMode eventsStudent
) {
}
