package com.gakkomobile.notifications;

import com.gakkomobile.avro.NotificationEventAvro;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class FirebaseMessagingPushService {

    private final UserDeviceTokenRepository deviceTokenRepository;

    public void send(NotificationEventAvro notificationEvent, UUID userId) {

        List<UserDeviceTokenEntity> tokens = deviceTokenRepository.findAllByUserId(userId);
        FirebaseMessageContextDTO messageContextDTO = new FirebaseMessageContextDTO(notificationEvent.getCategory());

        if (Objects.nonNull(notificationEvent.getTargetScreen())) {

            messageContextDTO.setTargetScreen(notificationEvent.getTargetScreen());
        }

        try {
            for (UserDeviceTokenEntity token : tokens) {
                Message message = buildMessage(token.getToken(), notificationEvent.getTitle(), notificationEvent.getPayload(), messageContextDTO);
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Successfully sent message: {}, with response:  {}", message, response);
            }
        } catch (Exception e) {
            log.error("Unable to send FCM Push message for user: {}", userId, e);
        }
    }

    private Message buildMessage(String deviceToken, String title, String body, FirebaseMessageContextDTO messageContextDTO) {

        Message.Builder messageBuilder = Message.builder()
                .setFid(deviceToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (Objects.nonNull(messageContextDTO)) {

            messageBuilder.putData("category", messageContextDTO.getCategory());
            messageBuilder.putData("targetScreen", messageContextDTO.getTargetScreen());
        }

        return messageBuilder.build();
    }
}
