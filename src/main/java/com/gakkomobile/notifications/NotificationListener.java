package com.gakkomobile.notifications;

import com.gakkomobile.avro.NotificationEventAvro;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.push.PushNotificationSender;
import com.gakkomobile.notifications.settings.UserNotificationSettingsService;
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
    private final UserNotificationSettingsService settingsService;
    private final PushNotificationSender pushNotificationSender;
    private final NotificationMapper notificationMapper;

    @KafkaListener(
            topics = "gakko.notifications",
            groupId = "gakko-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeNotification(NotificationEventAvro notificationEvent) {

        try {

            NotificationDTO notificationDTO = notificationMapper.avroToDTO(notificationEvent);

            if (Objects.isNull(notificationDTO.targetId())) {
                log.warn("Wrong payload. No target_id found in message: {}", notificationDTO.body());
                return;
            }

            UUID targetId = UUID.fromString(notificationDTO.targetId());
            NotificationMode mode = settingsService.getUserNotificationSettingsMode(notificationDTO, targetId);

            if (mode == NotificationMode.OFF) {

                return;
            }

            // Save in-app notification
            notificationService.createInAppNotification(notificationDTO);

            // Send push notification
            if (mode == NotificationMode.PUSH_AND_INAPP) {

                pushNotificationSender.send(notificationDTO, targetId);
            }

        } catch (Exception e) {
            log.error("Error while consuming notification", e);
        }
    }
}