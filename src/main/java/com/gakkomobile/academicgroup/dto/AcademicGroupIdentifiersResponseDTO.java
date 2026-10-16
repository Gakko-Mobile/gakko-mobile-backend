package com.gakkomobile.academicgroup.dto;

import java.util.UUID;

public record AcademicGroupIdentifiersResponseDTO(
        UUID id,
        String studiesName,
        String academicYear,
        String name
) {
}