package com.gakkomobile.notifications;

import java.time.OffsetDateTime;

record UserNotificationSettingsDTO(
  String userId,
  NotificationMode academicGrades,
  NotificationMode academicScheduleChanges,
  NotificationMode academicCancelledClasses,
  NotificationMode financialNewCharges,
  NotificationMode eventsStudent,
  OffsetDateTime updatedAt
) {
}
