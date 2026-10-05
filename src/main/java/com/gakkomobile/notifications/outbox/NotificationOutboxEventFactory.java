package com.gakkomobile.notifications.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationOutboxEventFactory {

    private final NotificationOutboxEventRepository outboxEventRepository;
    private final NotificationMapper notificationMapper;

    public void create(NotificationDTO notificationDTO) {

        NotificationOutboxEventEntity entity = notificationMapper.dtoToEntity(notificationDTO);
        outboxEventRepository.save(entity);
    }
}
