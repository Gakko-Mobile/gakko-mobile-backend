
package com.gakkomobile.notifications.dto;

import com.gakkomobile.notifications.outbox.NotificationTargetType;
import lombok.Builder;

/**
 * Data Transfer Object (DTO) representing a notification event payload
 * intended to be written into the transactional outbox table
 * ({@code outbox_events}).
 * <p>
 * This DTO acts as a contract between internal domain modules
 * (e.g., Grades, Schedule, Finance) and the Notifications module.
 * It decouples business logic from notification processing and prepares
 * structured data for asynchronous delivery through Apache Kafka and
 * mobile push notification services (FCM).
 *
 * @param aggregateType the domain entity or aggregate root type triggering
 *                      the notification.
 *                      Example: {@code "GRADE"}, {@code "SCHEDULE"},
 *                      {@code "PAYMENT"}, {@code "APPLICATION"}.
 * @param eventType the specific business action or state transition
 *                  that occurred.
 *                  Example: {@code "GRADE_CREATED"}, {@code "ROOM_CHANGED"},
 *                  {@code "PAYMENT_REMINDER"}.
 * @param targetType the recipient target scope, such as an individual user,
 *                   study group, or entire role.
 *                   Example: {@link NotificationTargetType#USER},
 *                   {@link NotificationTargetType#GROUP}.
 * @param targetId the identifier of the target recipient or recipients,
 *                 such as a student ID or group code.
 *                 Can be {@code null} if the target scope is global
 *                 or system-wide.
 *                 Example: {@code "s12345"} (student ID) or
 *                 {@code "WD-K23-01"} (group code).
 * @param title the user-facing title displayed in mobile push banners
 *              and notification lists.
 *              Example: {@code "New grade posted in ICK"},
 *              {@code "Room change alert"}.
 * @param body the detailed message content describing the event
 *             to the user.
 *             Example: {@code "You received a 5.0 in ICK Labs from Dr. Kowalski."}
 *             or {@code "Lecture moved from A503 to A203."}.
 * @param category the notification category used for user preference
 *                 filtering.
 *                 Example: {@code "ACADEMIC"}, {@code "FINANCIAL"},
 *                 {@code "EVENTS"}, {@code "COMMUNICATION"}.
 * @param targetScreen the optional deep-link destination screen key
 *                     within the React Native mobile application.
 *                     Example: {@code "grades-screen"},
 *                     {@code "calendar-screen"}, {@code "payments-screen"}.
 */
@Builder
public record NotificationDTO(
        String aggregateType,
        String aggregateId,
        String eventType,
        NotificationTargetType targetType,
        String targetId,
        String title,
        String body,
        String category,
        String targetScreen
) {
}
