package com.gakkomobile.building.dto;

import java.util.UUID;

public record BuildingIdentifiersResponseDTO(
        UUID id,
        String name
) {
}
