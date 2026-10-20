package com.gakkomobile.notifications.push;

import com.gakkomobile.notifications.dto.NotificationDTO;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.FirebaseMessagingExceptionTestFactory;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FirebaseMessagingPushServiceTest {

    @Mock
    private UserDeviceTokenRepository deviceTokenRepository;

    @Mock
    private FirebaseMessagingClient firebaseMessagingClient;

    @InjectMocks
    private FirebaseMessagingPushService pushService;

    private static final UUID USER_ID = UUID.fromString("01a11b16-7e4f-71f7-84e4-397e5d2d7c03");

    @Nested
    @DisplayName("Missing FCM Tokens Scenarios")
    class NoTokensTests {

        @Test
        @DisplayName("Should log warning and return early when no FCM tokens exist for user")
        void send_whenNoTokensFound_shouldNotCallFirebase() throws FirebaseMessagingException {
            // given
            NotificationDTO notificationDTO = createNotificationDTO();
            given(deviceTokenRepository.findAllByUserId(USER_ID)).willReturn(Collections.emptyList());

            // when
            pushService.send(notificationDTO, USER_ID);

            // then
            then(deviceTokenRepository).should().findAllByUserId(USER_ID);
            then(deviceTokenRepository).shouldHaveNoMoreInteractions();
            then(firebaseMessagingClient).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Successful Push Notification Scenarios")
    class SuccessfulSendTests {

        @Test
        @DisplayName("Should construct message and send push notification via FCM for each registered token")
        void send_whenTokensExist_shouldSendMessagesToFirebase() throws FirebaseMessagingException {
            // given
            NotificationDTO notificationDTO = createNotificationDTO();
            UserDeviceTokenEntity tokenEntity1 = createTokenEntity("token-1");
            UserDeviceTokenEntity tokenEntity2 = createTokenEntity("token-2");

            given(deviceTokenRepository.findAllByUserId(USER_ID))
                    .willReturn(List.of(tokenEntity1, tokenEntity2));
            given(firebaseMessagingClient.send(any(Message.class)))
                    .willReturn("projects/test/messages/12345");

            // when
            pushService.send(notificationDTO, USER_ID);

            // then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            then(firebaseMessagingClient).should(times(2)).send(messageCaptor.capture());

            List<Message> capturedMessages = messageCaptor.getAllValues();
            assertThat(capturedMessages).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Firebase Messaging Error Handling Scenarios")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should delete token from repository when Firebase throws UNREGISTERED exception")
        void send_whenFirebaseThrowsUnregisteredError_shouldDeleteTokenFromRepository() throws FirebaseMessagingException {
            // given
            NotificationDTO notificationDTO = createNotificationDTO();
            UserDeviceTokenEntity expiredTokenEntity = createTokenEntity("expired-token-123");

            given(deviceTokenRepository.findAllByUserId(USER_ID))
                    .willReturn(List.of(expiredTokenEntity));

            FirebaseMessagingException unregisteredException = FirebaseMessagingExceptionTestFactory.createUnregisteredException();
            given(firebaseMessagingClient.send(any(Message.class)))
                    .willThrow(unregisteredException);

            // when
            pushService.send(notificationDTO, USER_ID);

            // then
            then(deviceTokenRepository).should()
                    .deleteByUserIdAndToken(USER_ID, "expired-token-123");
        }

        @Test
        @DisplayName("Should log error and keep token in repository when Firebase throws generic exception")
        void send_whenFirebaseThrowsGenericError_shouldNotDeleteToken() throws FirebaseMessagingException {
            // given
            NotificationDTO notificationDTO = createNotificationDTO();
            UserDeviceTokenEntity tokenEntity = createTokenEntity("valid-token-123");

            given(deviceTokenRepository.findAllByUserId(USER_ID))
                    .willReturn(List.of(tokenEntity));

            FirebaseMessagingException genericException = FirebaseMessagingExceptionTestFactory.createGenericException();
            given(firebaseMessagingClient.send(any(Message.class)))
                    .willThrow(genericException);

            // when
            pushService.send(notificationDTO, USER_ID);

            // then
            then(deviceTokenRepository).should(never())
                    .deleteByUserIdAndToken(any(), any());
        }
    }

    private NotificationDTO createNotificationDTO() {
        return NotificationDTO.builder()
                .title("Classroom Change")
                .body("Class moved to room A203")
                .category("ACADEMIC")
                .targetScreen("calendar-screen")
                .build();
    }

    private UserDeviceTokenEntity createTokenEntity(String token) {
        UserDeviceTokenEntity entity = new UserDeviceTokenEntity();
        entity.setUserId(USER_ID);
        entity.setToken(token);
        return entity;
    }
}