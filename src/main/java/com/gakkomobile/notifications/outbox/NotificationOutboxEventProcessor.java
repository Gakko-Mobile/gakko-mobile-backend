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

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxEventProcessor {

    private final NotificationOutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, NotificationEventAvro> kafkaTemplate;
    private final NotificationMapper notificationMapper;

    @Value("${kafka.topic.notifications}")
    private String notificationTopic;

    @Scheduled(fixedDelay = 1000) // 1 second
    @Transactional
    public void processOutboxNotifications() {
        List<NotificationOutboxEventEntity> unprocessedEvents = outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();

        for (NotificationOutboxEventEntity event : unprocessedEvents) {
            try {
                NotificationEventAvro notificationEvent = notificationMapper.entityToAvro(event);
                kafkaTemplate.send(notificationTopic, event.getAggregateId(), notificationEvent)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setProcessed(true);
                                outboxRepository.save(event);
                                log.debug("Wysłano zdarzenie Outbox do Kafki: {}", event.getId());
                            } else {
                                log.error("Błąd wysyłki zdarzenia do Kafki: {}", event.getId(), ex);
                            }
                        });
            } catch (Exception e) {
                log.error("Błąd przetwarzania zdarzenia Outbox: {}", event.getId(), e);
            }
        }
    }
}
