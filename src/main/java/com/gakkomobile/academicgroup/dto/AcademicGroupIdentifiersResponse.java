package com.gakkomobile.academicgroup.dto;

import java.util.UUID;

public record AcademicGroupIdentifiersResponse(
        UUID id,
        String studiesName,
        String academicYear,
        String name
) {
}