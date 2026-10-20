package com.gakkomobile.notifications.outbox;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.gakkomobile.BaseIntegrationTest;
import com.gakkomobile.avro.NotificationEventAvro;
import com.gakkomobile.common.event.EventAggregationType;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.CloseOptions;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class NotificationOutboxEventProcessorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationOutboxEventProcessor outboxProcessor;

    @Autowired
    private NotificationOutboxEventRepository outboxEventRepository;

    @Value("${kafka.topic.notifications}")
    private String notificationTopic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.schema-registry-url}")
    private String schemaRegistryUrl;

    private Consumer<String, NotificationEventAvro> testConsumer;
    private final List<ConsumerRecord<String, NotificationEventAvro>> received = new ArrayList<>();

    @BeforeEach
    void setUpTestConsumer() {

        createDefaultNotificationSettings(currentUser.getId());

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "outbox-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        testConsumer = new DefaultKafkaConsumerFactory<String, NotificationEventAvro>(props).createConsumer();

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(300))
                .pollInSameThread()
                .ignoreExceptions()
                .until(() -> !testConsumer.partitionsFor(notificationTopic).isEmpty());

        List<TopicPartition> partitions = testConsumer.partitionsFor(notificationTopic).stream()
                .map(info -> new TopicPartition(info.topic(), info.partition()))
                .toList();

        testConsumer.assign(partitions);
        testConsumer.seekToBeginning(partitions);

        received.clear();
    }

    @AfterEach
    void tearDown() {

        if (testConsumer != null) {
            testConsumer.close(CloseOptions.timeout(Duration.ofSeconds(2)));
        }
    }

    @Test
    @DisplayName("Should send unprocessed outbox events to Kafka as Avro and mark them as processed")
    void processOutboxNotifications_shouldPublishToKafkaAndMarkAsProcessed() {

        // given
        String key1 = "sched-" + uuid(3);
        String key2 = "survey-" + uuid(4);

        NotificationOutboxEventEntity entity1 = persistAndCommit(
                createOutboxEntity(EventAggregationType.SCHEDULE.toString(), key1, "Classroom Change", false));
        NotificationOutboxEventEntity entity2 = persistAndCommit(
                createOutboxEntity(EventAggregationType.SURVEY.toString(), key2, "New Survey Added", false));

        // when
        outboxProcessor.processOutboxNotifications();

        // then
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(outboxEventRepository.findById(entity1.getId()).orElseThrow().isProcessed()).isTrue();
                    assertThat(outboxEventRepository.findById(entity2.getId()).orElseThrow().isProcessed()).isTrue();
                });

        // then
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .pollInSameThread()
                .untilAsserted(() -> {
                    drainConsumer();

                    assertThat(received).extracting(ConsumerRecord::key).contains(key1, key2);
                    assertThat(titleFor(key1)).isEqualTo("Classroom Change");
                    assertThat(titleFor(key2)).isEqualTo("New Survey Added");
                });
    }

    @Test
    @DisplayName("Should ignore already processed outbox events")
    void processOutboxNotifications_shouldIgnoreAlreadyProcessedEvents() {

        // given
        String key = "sched-" + uuid(5);
        persistAndCommit(createOutboxEntity("SCHEDULE", key, "Old Event", true));

        // when
        outboxProcessor.processOutboxNotifications();

        // then
        long deadline = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < deadline) {
            drainConsumer();
        }

        assertThat(received).extracting(ConsumerRecord::key).doesNotContain(key);
    }


    private void drainConsumer() {
        testConsumer.poll(Duration.ofMillis(500)).forEach(received::add);
    }

    private String titleFor(String key) {
        return received.stream()
                .filter(r -> key.equals(r.key()))
                .map(r -> String.valueOf(r.value().getTitle()))
                .findFirst()
                .orElseThrow();
    }

    private NotificationOutboxEventEntity createOutboxEntity(String aggregateType,
                                                             String aggregateId,
                                                             String title,
                                                             boolean processed) {

        NotificationOutboxEventEntity entity = new NotificationOutboxEventEntity();
        entity.setAggregateType(aggregateType);
        entity.setAggregateId(aggregateId);
        entity.setEventType("EVENT_CREATED");
        entity.setTargetType(NotificationTargetType.USER);
        entity.setTargetId(currentUser.getId().toString());
        entity.setPayload(new NotificationPayloadDTO(title, "Body", "ACADEMIC_GROUPS", "home"));
        entity.setProcessed(processed);
        return entity;
    }
}