package com.gakkomobile.room.dto;

import java.util.UUID;

public record RoomIdentifiersResponseDTO(
        UUID id,
        String buildingName,
        String name
) {
}
