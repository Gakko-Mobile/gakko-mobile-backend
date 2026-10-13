package com.gakkomobile.notifications.firebase.dto;

import com.gakkomobile.notifications.DevicePlatform;

import java.time.Instant;
import java.util.UUID;

public record UserDeviceTokenResponseDTO(
        UUID id,
        String token,
        DevicePlatform platform,
        Instant lastUpdatedAt
) {}
