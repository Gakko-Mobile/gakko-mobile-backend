package com.gakkomobile.notifications.outbox;

import com.gakkomobile.notifications.dto.NotificationDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationOutboxEventFactory {

    private final NotificationOutboxEventRepository outboxEventRepository;
    private final NotificationOutboxMapper notificationOutboxMapper;

    @Transactional
    public void create(NotificationDTO notificationDTO) {

        NotificationOutboxEventEntity entity = notificationOutboxMapper.dtoToEntity(notificationDTO);
        outboxEventRepository.save(entity);
    }
}
