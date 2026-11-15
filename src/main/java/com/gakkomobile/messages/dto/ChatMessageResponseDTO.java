package com.gakkomobile.messages.dto;

import com.gakkomobile.messages.domain.MessageStatus;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponseDTO(
        UUID id,
        UUID threadId,
        UUID senderId,
        String senderName,
        String content,
        boolean isUrgent,
        boolean isBuffered,
        Instant scheduledDeliveryAt,
        MessageStatus status,
        Instant createdAt
) {
}
