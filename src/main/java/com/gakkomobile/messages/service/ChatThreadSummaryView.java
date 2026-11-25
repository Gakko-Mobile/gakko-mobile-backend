package com.gakkomobile.messages.service;

import java.time.OffsetDateTime;
import java.util.UUID;

interface ChatThreadSummaryView {

    UUID getThreadId();

    String getSubject();

    String getThreadType();

    OffsetDateTime getLastMessageAt();

    String getLastMessageContent();

    long getUnreadCount();
}
