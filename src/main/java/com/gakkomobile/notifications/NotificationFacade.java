package com.gakkomobile.notifications;

import com.gakkomobile.notifications.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationService notificationService;

    public void sendNotification(NotificationDTO notificationDTO) {

        notificationService.putNotificationToOutbox(notificationDTO);
    }
}
