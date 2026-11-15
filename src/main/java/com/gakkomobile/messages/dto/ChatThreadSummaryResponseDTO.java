package com.gakkomobile.messages.dto;

import com.gakkomobile.messages.domain.ThreadType;

import java.time.Instant;
import java.util.UUID;

public record ChatThreadSummaryResponseDTO(
        UUID threadId,
        String subject,
        ThreadType type,
        Instant lastMessageAt,
        String lastMessageContent,
        long unreadCount
) {
}
