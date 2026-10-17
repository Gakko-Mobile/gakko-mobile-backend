package com.gakkomobile.notifications.firebase;

import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.PushNotificationSender;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
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
public class FirebaseMessagingPushService implements PushNotificationSender {

    private final UserDeviceTokenRepository deviceTokenRepository;

    public void send(NotificationDTO notificationDTO, UUID userId) {

        List<UserDeviceTokenEntity> tokens = deviceTokenRepository.findAllByUserId(userId);

        if (tokens.isEmpty()) {

            log.warn("No FCM tokens found for user: {}", userId);
            return;
        }

        FirebaseMessageContextDTO messageContextDTO = new FirebaseMessageContextDTO(notificationDTO.category(), notificationDTO.targetScreen());

        for (UserDeviceTokenEntity token : tokens) {
            Message message = buildMessage(token.getToken(), notificationDTO, messageContextDTO);
            sendMessageToFirebase(message, token, userId.toString());
        }
    }

    private record FirebaseMessageContextDTO(String category, String targetScreen) { }

    private Message buildMessage(String deviceToken, NotificationDTO notificationDTO,
                                 FirebaseMessageContextDTO messageContextDTO) {

        Message.Builder messageBuilder = Message.builder()
                .setFid(deviceToken)
                .setNotification(Notification.builder()
                        .setTitle(notificationDTO.title())
                        .setBody(notificationDTO.body())
                        .build());

        if (Objects.nonNull(messageContextDTO)) {

            messageBuilder.putData("category", messageContextDTO.category());
            messageBuilder.putData("targetScreen", messageContextDTO.targetScreen());
        }

        return messageBuilder.build();
    }

    private void sendMessageToFirebase(Message message, UserDeviceTokenEntity token, String userId) {

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent message: {}, with response:  {}", message, response);
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("FCM token is invalid or expired. Deleting from database for user: {}", userId);
                deviceTokenRepository.deleteByUserIdAndToken(token.getUserId(), token.getToken());
            } else {
                log.error("Unable to send FCM Push message for user: {}", userId, e);
            }
        }
    }
}
