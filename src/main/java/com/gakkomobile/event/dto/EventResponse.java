package com.gakkomobile.event.dto;

import java.util.UUID;

public record EventResponse(
        UUID id,
        String organizator,
        String description,
        String buildingName,
        String eventDate,
        String startTime,
        String endTime
) {
}