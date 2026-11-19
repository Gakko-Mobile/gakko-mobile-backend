package com.gakkomobile.subject.dto;

import java.util.UUID;

public record SubjectIdentifiersResponseDTO(
        UUID id,
        String code,
        String name
) {
}
