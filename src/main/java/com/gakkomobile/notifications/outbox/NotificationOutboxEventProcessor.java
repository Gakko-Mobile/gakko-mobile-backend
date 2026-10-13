package com.gakkomobile.notifications.outbox;


import com.gakkomobile.avro.NotificationEventAvro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxEventProcessor {

    private final NotificationOutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, NotificationEventAvro> kafkaTemplate;
    private final NotificationOutboxMapper notificationOutboxMapper;

    @Value("${kafka.topic.notifications}")
    private String notificationTopic;

    @Scheduled(fixedDelay = 1000) // 1 second
    @Transactional
    public void processOutboxNotifications() {
        List<NotificationOutboxEventEntity> unprocessedEvents = outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();

        for (NotificationOutboxEventEntity event : unprocessedEvents) {
            try {
                NotificationEventAvro notificationEvent = notificationOutboxMapper.entityToAvro(event);
                kafkaTemplate.send(notificationTopic, event.getAggregateId(), notificationEvent)
                        .whenComplete((_, ex) -> {
                            if (Objects.isNull(ex)) {
                                event.setProcessed(true);
                                outboxRepository.save(event);
                                log.debug("Send outbox event to Kafka: {}", event.getId());
                            } else {
                                log.error("Error while sending outbox event to Kafka: {}", event.getId(), ex);
                            }
                        });
            } catch (Exception e) {
                log.error("Error while sending outbox event to Kafka: {}", event.getId(), e);
            }
        }
    }
}
