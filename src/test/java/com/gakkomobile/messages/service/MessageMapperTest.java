package com.gakkomobile.messages.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gakkomobile.messages.domain.MessageStatus;
import com.gakkomobile.messages.domain.ThreadType;
import com.gakkomobile.messages.dto.ChatMessageResponseDTO;
import com.gakkomobile.messages.dto.ChatThreadSummaryResponseDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

class MessageMapperTest {

    private final MessageMapper messageMapper = new MessageMapperImpl();

    @Test
    void toResponse_shouldCopyFieldsAndConvertTimestamps() {

        UUID messageId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        OffsetDateTime scheduled = OffsetDateTime.of(2026, 11, 12, 8, 0, 0, 0, ZoneOffset.ofHours(1));
        OffsetDateTime created = OffsetDateTime.of(2026, 11, 11, 20, 0, 0, 0, ZoneOffset.ofHours(1));
        ChatMessageEntity message = new ChatMessageEntity();
        message.setId(messageId);
        message.setThreadId(threadId);
        message.setSenderId(senderId);
        message.setContent("Hello");
        message.setUrgent(true);
        message.setBuffered(true);
        message.setScheduledDeliveryAt(scheduled);
        message.setStatus(MessageStatus.BUFFERED);
        message.setCreatedAt(created);

        ChatMessageResponseDTO response = messageMapper.toResponse(message, "Ada Nowak");

        assertThat(response).isEqualTo(new ChatMessageResponseDTO(
                messageId,
                threadId,
                senderId,
                "Ada Nowak",
                "Hello",
                true,
                true,
                Instant.parse("2026-11-12T07:00:00Z"),
                MessageStatus.BUFFERED,
                Instant.parse("2026-11-11T19:00:00Z")
        ));
    }

    @Test
    void toResponse_shouldLeaveMissingTimestampsNull() {

        ChatMessageEntity message = new ChatMessageEntity();
        message.setId(UUID.randomUUID());
        message.setContent("Hello");
        message.setStatus(MessageStatus.DELIVERED);

        ChatMessageResponseDTO response = messageMapper.toResponse(message, null);

        assertThat(response.senderName()).isNull();
        assertThat(response.scheduledDeliveryAt()).isNull();
        assertThat(response.createdAt()).isNull();
    }

    @Test
    void toNewThreadSummary_shouldOmitLastMessageAndUnreadCount() {

        UUID threadId = UUID.randomUUID();
        ChatThreadEntity thread = new ChatThreadEntity();
        thread.setId(threadId);
        thread.setSubject("Consultation");
        thread.setThreadType(ThreadType.GROUP);

        assertThat(messageMapper.toNewThreadSummary(thread)).isEqualTo(new ChatThreadSummaryResponseDTO(
                threadId,
                "Consultation",
                ThreadType.GROUP,
                null,
                null,
                0
        ));
    }

    @Test
    void toSummary_shouldMapTheRepositoryView() {

        UUID threadId = UUID.randomUUID();
        OffsetDateTime lastMessageAt = OffsetDateTime.parse("2026-11-11T09:00:00Z");
        ChatThreadSummaryView view = new ChatThreadSummaryView() {
            @Override
            public UUID getThreadId() {
                return threadId;
            }

            @Override
            public String getSubject() {
                return "Consultation";
            }

            @Override
            public String getThreadType() {
                return ThreadType.OFFICIAL.name();
            }

            @Override
            public OffsetDateTime getLastMessageAt() {
                return lastMessageAt;
            }

            @Override
            public String getLastMessageContent() {
                return "Hello";
            }

            @Override
            public long getUnreadCount() {
                return 2;
            }
        };

        assertThat(messageMapper.toSummary(view)).isEqualTo(new ChatThreadSummaryResponseDTO(
                threadId,
                "Consultation",
                ThreadType.OFFICIAL,
                Instant.parse("2026-11-11T09:00:00Z"),
                "Hello",
                2
        ));
    }
}
