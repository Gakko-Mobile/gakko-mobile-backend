package com.gakkomobile.studies.dto;

import java.util.UUID;

public record StudiesResponse(
        UUID id,
        String name
) {
}
