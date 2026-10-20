package com.gakkomobile.notifications.push;

import com.gakkomobile.notifications.dto.NotificationDTO;

import java.util.UUID;

public interface PushNotificationSender {

    void send(NotificationDTO notificationDTO, UUID targetId);
}
