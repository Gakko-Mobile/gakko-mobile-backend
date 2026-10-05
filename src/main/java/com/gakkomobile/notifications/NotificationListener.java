package com.gakkomobile.notifications;

import com.gakkomobile.avro.NotificationEventAvro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class NotificationListener {

    private final NotificationService notificationService;
    private final UserNotificationSettingsRepository settingsRepository;
    private final UserDeviceTokenRepository deviceTokenRepository;
    private final FirebaseMessagingPushService firebaseMessagingPushService;

    @KafkaListener(
            topics = "gakko.notifications",
            groupId = "gakko-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeNotification(NotificationEventAvro notificationEvent) {

        try {

            if (Objects.isNull(notificationEvent.getTargetId())) {
                log.warn("Wrong payload. No target_id found in message: {}", notificationEvent.getPayload());
                return;
            }

            UUID userId = UUID.fromString(notificationEvent.getTargetId());

            UserNotificationSettingsEntity settings =
                    settingsRepository.findById(userId)
                            .orElseGet(() -> {
                                UserNotificationSettingsEntity defaults =
                                        new UserNotificationSettingsEntity();
                                defaults.setUserId(userId);
                                return defaults;
                            });

            NotificationMode mode = resolveMode(settings, NotificationCategory.valueOf(notificationEvent.getCategory()));

            if (mode == NotificationMode.OFF) {
                return;
            }

            // Save in-app notification
            notificationService.createInAppNotification(notificationEvent);

            // Send push notification
            if (mode == NotificationMode.PUSH_AND_INAPP) {

                firebaseMessagingPushService.send(notificationEvent, userId);
            }

        } catch (Exception e) {
            log.error("Error while consuming notification", e);
        }
    }

    private NotificationMode resolveMode(UserNotificationSettingsEntity settings, NotificationCategory category) {

        return switch (category) {
            case ACADEMIC_GRADES -> settings.getAcademicGrades();
            case ACADEMIC_SCHEDULE_CHANGES -> settings.getAcademicScheduleChanges();
            case ACADEMIC_CANCELLED_CLASSES -> settings.getAcademicCancelledClasses();
            case FINANCIAL_NEW_CHARGES -> settings.getFinancialNewCharges();
            case EVENTS_STUDENT -> settings.getEventsStudent();
        };
    }
}