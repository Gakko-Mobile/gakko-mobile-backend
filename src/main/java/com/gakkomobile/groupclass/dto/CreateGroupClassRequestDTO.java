package com.gakkomobile.groupclass.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreateGroupClassRequestDTO(
        @NotNull UUID subjectId,
        @NotNull UUID roomId,
        @NotEmpty Set<@NotNull UUID> groupIds,
        @NotEmpty Set<@NotNull UUID> lecturerIds,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull Boolean oneTime,
        @NotEmpty List<@NotNull @Valid CreateClassSlotRequestDTO> slots
) {
}
