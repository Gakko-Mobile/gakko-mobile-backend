package com.gakkomobile.groupclass.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record UpdateGroupClassRequestDTO(
        @NotNull UUID subjectId,
        @NotNull UUID roomId,
        @NotEmpty Set<@NotNull UUID> groupIds,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {
}
