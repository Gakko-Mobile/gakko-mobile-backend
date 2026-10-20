package com.gakkomobile.notifications.outbox;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.gakkomobile.notifications.dto.NotificationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxEventFactoryTest {

    @Mock
    private NotificationOutboxEventRepository outboxEventRepository;

    @Mock
    private NotificationOutboxMapper notificationOutboxMapper;

    @InjectMocks
    private NotificationOutboxEventFactory notificationOutboxEventFactory;

    @Test
    void create_shouldDelegateToOutboxEventRepository() {

        // given
        NotificationDTO notificationDTO = NotificationDTO.builder().build();
        NotificationOutboxEventEntity entity = new NotificationOutboxEventEntity();
        when(notificationOutboxMapper.dtoToEntity(notificationDTO)).thenReturn(entity);

        // when
        notificationOutboxEventFactory.create(notificationDTO);

        // then
        verify(outboxEventRepository).save(entity);
    }
}
