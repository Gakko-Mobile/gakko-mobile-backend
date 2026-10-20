package com.gakkomobile.notifications.push.dto;

import com.gakkomobile.notifications.DevicePlatform;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserDeviceTokenResponseDTO(
        UUID id,
        String token,
        DevicePlatform platform,
        Instant lastUpdatedAt
) {}
