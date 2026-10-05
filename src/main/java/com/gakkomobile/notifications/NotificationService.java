package com.gakkomobile.notifications;

import com.gakkomobile.avro.NotificationEventAvro;
import com.gakkomobile.notifications.outbox.NotificationDTO;
import com.gakkomobile.notifications.outbox.NotificationOutboxEventFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final NotificationOutboxEventFactory notificationOutboxEventFactory;

    @Transactional
    public void createOutboxNotification(NotificationDTO notificationDTO) {

        notificationOutboxEventFactory.create(notificationDTO);
    }

    @Transactional
    public void createInAppNotification(NotificationEventAvro notificationEvent) {

        UserNotificationEntity notification = new UserNotificationEntity();
        notification.setUserId(UUID.fromString(notificationEvent.getTargetId()));
        notification.setTitle(notificationEvent.getTitle());
        notification.setBody(notificationEvent.getPayload());
        notification.setCategory(notificationEvent.getCategory());
        notificationRepository.save(notification);
    }
}
