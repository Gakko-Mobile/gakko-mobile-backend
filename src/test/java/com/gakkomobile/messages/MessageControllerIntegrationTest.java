package com.gakkomobile.messages;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gakkomobile.BaseIntegrationTest;
import com.gakkomobile.messages.domain.MessageStatus;
import com.gakkomobile.messages.domain.ThreadType;
import com.gakkomobile.messages.dto.ChatMessageResponseDTO;
import com.gakkomobile.messages.dto.ChatThreadSummaryResponseDTO;
import com.gakkomobile.messages.dto.CreateThreadRequestDTO;
import com.gakkomobile.messages.dto.SendMessagePayloadDTO;
import com.gakkomobile.messages.service.MessageService;
import com.gakkomobile.security.jwt.JwtService;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

class MessageControllerIntegrationTest extends BaseIntegrationTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final String LECTURER_TOKEN = "lecturer-token";
    private static final String OUTSIDER_TOKEN = "outsider-token";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private java.time.Clock clock;

    private WebTestClient lecturerClient;
    private WebTestClient outsiderClient;

    @BeforeEach
    void setUpMessenger() {

        when(jwtService.extractUsername(anyString())).thenAnswer(invocation -> switch (invocation.getArgument(0, String.class)) {
            case LECTURER_TOKEN -> "lecturer@pjatk.edu.pl";
            case OUTSIDER_TOKEN -> "outsider@pjatk.edu.pl";
            default -> "student@pjatk.edu.pl";
        });
        freeze(LocalDateTime.of(2026, 11, 11, 10, 0));

        UserEntity lecturer = createAndSaveUser(uuid(1), "lecturer@pjatk.edu.pl");
        lecturer.setRole(Role.LECTURER);
        userRepository.saveAndFlush(lecturer);
        createAndSaveUser(uuid(2), "outsider@pjatk.edu.pl");

        lecturerClient = clientFor(LECTURER_TOKEN);
        outsiderClient = clientFor(OUTSIDER_TOKEN);
    }

    @Test
    @DisplayName("Delivers a working-hours message over HTTP and writes an outbox event")
    void sendDuringWorkingHours_shouldReturnDeliveredMessageAndNotify() {

        UUID threadId = createConsultationThread();

        ChatMessageResponseDTO message = send(authenticatedClient, threadId, "Hello lecturer", false);

        assertThat(message.content()).isEqualTo("Hello lecturer");
        assertThat(message.isBuffered()).isFalse();
        assertThat(message.status()).isEqualTo(MessageStatus.DELIVERED);
        assertThat(message.senderName()).isEqualTo("John Doe");
        assertThat(isBuffered(threadId)).isFalse();
        assertThat(outboxCount(message.id())).isEqualTo(1L);
        assertVisible(lecturerClient, threadId, "Hello lecturer", false, MessageStatus.DELIVERED);

        lecturerClient.post()
                .uri("/api/v1/messages/threads/{threadId}/read", threadId)
                .exchange()
                .expectStatus().isNoContent();
        assertThat(lastReadAt(threadId, uuid(1))).isEqualTo(at(2026, 11, 11, 10, 0));
    }

    @Test
    @DisplayName("Buffers a non-urgent student message outside working hours until the scheduler releases it")
    void sendOutsideWorkingHours_shouldHideMessageUntilSchedulerRuns() {

        freeze(LocalDateTime.of(2026, 11, 11, 20, 0));
        UUID threadId = createConsultationThread();

        ChatMessageResponseDTO buffered = send(authenticatedClient, threadId, "After hours", false);

        assertThat(buffered.isBuffered()).isTrue();
        assertThat(buffered.status()).isEqualTo(MessageStatus.BUFFERED);
        assertThat(isBuffered(threadId)).isTrue();
        assertThat(messageStatus(threadId)).isEqualTo("BUFFERED");
        assertThat(scheduledDelivery(threadId)).isEqualTo(at(2026, 11, 12, 8, 0));
        assertThat(outboxCountForThread(threadId)).isZero();
        assertThat(visibleCount(lecturerClient, threadId)).isZero();
        assertVisible(authenticatedClient, threadId, "After hours", true, MessageStatus.BUFFERED);

        freeze(LocalDateTime.of(2026, 11, 12, 8, 0));
        messageService.releaseDueMessages();

        assertThat(isBuffered(threadId)).isFalse();
        assertVisible(lecturerClient, threadId, "After hours", false, MessageStatus.DELIVERED);
        assertThat(outboxCount(buffered.id())).isEqualTo(1L);
    }

    @Test
    @DisplayName("Rejects HTTP access to a thread the user does not belong to")
    void accessForeignThread_shouldFailAndAllowOwnThread() {

        UUID foreignThreadId = createConsultationThread();
        ChatThreadSummaryResponseDTO ownThread = outsiderClient.post()
                .uri("/api/v1/messages/threads")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateThreadRequestDTO("Own notes", ThreadType.DIRECT, java.util.List.of()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ChatThreadSummaryResponseDTO.class)
                .returnResult()
                .getResponseBody();
        assertThat(ownThread).isNotNull();

        outsiderClient.post()
                .uri("/api/v1/messages/threads/{threadId}/messages", foreignThreadId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SendMessagePayloadDTO("Not allowed", false))
                .exchange()
                .expectStatus().isForbidden();
        outsiderClient.get()
                .uri("/api/v1/messages/threads/{threadId}/messages", foreignThreadId)
                .exchange()
                .expectStatus().isForbidden();
        outsiderClient.post()
                .uri("/api/v1/messages/threads/{threadId}/read", foreignThreadId)
                .exchange()
                .expectStatus().isForbidden();

        ChatMessageResponseDTO ownMessage = send(outsiderClient, ownThread.threadId(), "My note", false);
        assertThat(ownMessage.status()).isEqualTo(MessageStatus.DELIVERED);
        assertVisible(outsiderClient, ownThread.threadId(), "My note", false, MessageStatus.DELIVERED);
    }

    private WebTestClient clientFor(String token) {

        return webTestClient.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    private UUID createConsultationThread() {

        ChatThreadSummaryResponseDTO created = authenticatedClient.post()
                .uri("/api/v1/messages/threads")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateThreadRequestDTO("Consultation", ThreadType.DIRECT, java.util.List.of(uuid(1))))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ChatThreadSummaryResponseDTO.class)
                .returnResult()
                .getResponseBody();
        assertThat(created).isNotNull();
        return created.threadId();
    }

    private ChatMessageResponseDTO send(WebTestClient client, UUID threadId, String content, boolean urgent) {

        ChatMessageResponseDTO response = client.post()
                .uri("/api/v1/messages/threads/{threadId}/messages", threadId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SendMessagePayloadDTO(content, urgent))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ChatMessageResponseDTO.class)
                .returnResult()
                .getResponseBody();
        assertThat(response).isNotNull();
        return response;
    }

    private void assertVisible(
            WebTestClient client,
            UUID threadId,
            String content,
            boolean buffered,
            MessageStatus status
    ) {

        client.get()
                .uri("/api/v1/messages/threads/{threadId}/messages", threadId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.content[0].content").isEqualTo(content)
                .jsonPath("$.content[0].isBuffered").isEqualTo(buffered)
                .jsonPath("$.content[0].status").isEqualTo(status.name());
    }

    private int visibleCount(WebTestClient client, UUID threadId) {

        Integer count = client.get()
                .uri("/api/v1/messages/threads/{threadId}/messages", threadId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(VisiblePage.class)
                .returnResult()
                .getResponseBody()
                .totalElements();
        return count == null ? 0 : count;
    }

    private void freeze(LocalDateTime localDateTime) {

        ZonedDateTime zoned = localDateTime.atZone(WARSAW);
        when(clock.instant()).thenReturn(zoned.toInstant());
        when(clock.getZone()).thenReturn(WARSAW);
    }

    private static Instant at(int year, int month, int day, int hour, int minute) {

        return LocalDateTime.of(year, month, day, hour, minute).atZone(WARSAW).toInstant();
    }

    private Boolean isBuffered(UUID threadId) {

        return jdbcTemplate.queryForObject(
                "SELECT is_buffered FROM chat_messages WHERE thread_id = ?",
                Boolean.class,
                threadId
        );
    }

    private String messageStatus(UUID threadId) {

        return jdbcTemplate.queryForObject(
                "SELECT status FROM chat_messages WHERE thread_id = ?",
                String.class,
                threadId
        );
    }

    private Instant scheduledDelivery(UUID threadId) {

        return jdbcTemplate.query(
                "SELECT scheduled_delivery_at FROM chat_messages WHERE thread_id = ?",
                resultSet -> {
                    if (!resultSet.next()) {
                        return null;
                    }
                    OffsetDateTime scheduled = resultSet.getObject(1, OffsetDateTime.class);
                    return scheduled.toInstant();
                },
                threadId
        );
    }

    private Instant lastReadAt(UUID threadId, UUID userId) {

        return jdbcTemplate.query(
                "SELECT last_read_at FROM chat_thread_participants WHERE thread_id = ? AND user_id = ?",
                resultSet -> {
                    if (!resultSet.next()) {
                        return null;
                    }
                    OffsetDateTime readAt = resultSet.getObject(1, OffsetDateTime.class);
                    return readAt.toInstant();
                },
                threadId,
                userId
        );
    }

    private Long outboxCount(UUID messageId) {

        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM outbox_events
                WHERE aggregate_type = 'MESSAGE'
                  AND event_type = 'MESSAGE_CREATED'
                  AND aggregate_id = ?
                  AND payload->>'category' = 'COMMUNICATION'
                """, Long.class, messageId.toString());
    }

    private Long outboxCountForThread(UUID threadId) {

        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM outbox_events
                WHERE aggregate_type = 'MESSAGE'
                  AND aggregate_id IN (
                      SELECT id::text FROM chat_messages WHERE thread_id = ?
                  )
                """, Long.class, threadId);
    }

    private record VisiblePage(Integer totalElements) {
    }
}
