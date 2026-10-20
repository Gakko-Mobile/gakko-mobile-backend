package com.gakkomobile.notifications;

import static org.mockito.Mockito.verify;
import com.gakkomobile.notifications.dto.NotificationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationFacadeTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationFacade notificationFacade;

    @Test
    void sendNotification_shouldDelegateToNotificationService() {

        // given
        NotificationDTO notificationDTO = NotificationDTO.builder().build();

        // when
        notificationFacade.sendNotification(notificationDTO);

        // then
        verify(notificationService).putNotificationToOutbox(notificationDTO);
    }

}
