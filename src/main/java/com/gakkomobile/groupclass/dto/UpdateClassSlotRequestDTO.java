package com.gakkomobile.groupclass.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateClassSlotRequestDTO(
        @NotNull LocalDate classDate,
        @Size(max = 100) String msTeamsCode
) {
}
