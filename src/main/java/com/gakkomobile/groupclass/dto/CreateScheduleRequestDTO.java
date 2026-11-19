package com.gakkomobile.groupclass.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record CreateScheduleRequestDTO(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotEmpty Set<@NotNull @Valid ScheduleGroupClassDTO> groupClasses
) {

}
