package com.gakkomobile.notifications.dto;

import java.util.UUID;

public record InAppNotificationReadInfoResponseDTO(
        UUID id,
        boolean isRead
){ }