package com.gakkomobile.notifications;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gakkomobile.avro.NotificationEventAvro;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.push.PushNotificationSender;
import com.gakkomobile.notifications.settings.UserNotificationSettingsService;
import com.gakkomobile.user.AppLanguage;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    private static final UUID RECIPIENT_ID = uuid(2);

    @Mock
    private NotificationService notificationService;
    @Mock
    private UserNotificationSettingsService settingsService;
    @Mock
    private PushNotificationSender pushNotificationSender;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationTextResolver notificationTextResolver;

    @InjectMocks
    private NotificationListener listener;

    @Test
    void consumeNotification_shouldPushTheRecipientLanguageAndKeepTheStoredMessageKey() {

        NotificationDTO stored = notification();
        UserEntity recipient = new UserEntity();
        recipient.setId(RECIPIENT_ID);
        recipient.setAppLanguage(AppLanguage.EN);
        when(notificationMapper.avroToDTO(org.mockito.ArgumentMatchers.any())).thenReturn(stored);
        when(settingsService.getUserNotificationSettingsMode(stored, RECIPIENT_ID)).thenReturn(NotificationMode.PUSH_AND_INAPP);
        when(userRepository.findById(RECIPIENT_ID)).thenReturn(Optional.of(recipient));
        when(notificationTextResolver.resolve(NotificationMessages.MESSAGE_CREATED, Locale.ENGLISH)).thenReturn("New message");

        listener.consumeNotification(event());

        verify(notificationService).createInAppNotification(stored);
        verify(pushNotificationSender).send(stored, "New message", RECIPIENT_ID);
    }

    @Test
    void consumeNotification_shouldUsePolishWhenTheRecipientHasNoSavedLanguage() {

        NotificationDTO stored = notification();
        when(notificationMapper.avroToDTO(org.mockito.ArgumentMatchers.any())).thenReturn(stored);
        when(settingsService.getUserNotificationSettingsMode(stored, RECIPIENT_ID)).thenReturn(NotificationMode.PUSH_AND_INAPP);
        when(userRepository.findById(RECIPIENT_ID)).thenReturn(Optional.empty());
        when(notificationTextResolver.resolve(NotificationMessages.MESSAGE_CREATED, Locale.forLanguageTag("pl")))
                .thenReturn("Nowa wiadomość");

        listener.consumeNotification(event());

        verify(pushNotificationSender).send(stored, "Nowa wiadomość", RECIPIENT_ID);
    }

    private static NotificationDTO notification() {

        return NotificationDTO.builder()
                .targetId(RECIPIENT_ID.toString())
                .body("Hello")
                .messageKey(NotificationMessages.MESSAGE_CREATED)
                .build();
    }

    private static NotificationEventAvro event() {

        return NotificationEventAvro.newBuilder()
                .setEventId("event-1")
                .setAggregateId("message-1")
                .setAggregateType("MESSAGE")
                .setEventType("MESSAGE_CREATED")
                .setTargetType("USER")
                .setTargetId(RECIPIENT_ID.toString())
                .setBody("Hello")
                .setCategory("COMMUNICATION")
                .setMessageKey(NotificationMessages.MESSAGE_CREATED)
                .build();
    }
}
