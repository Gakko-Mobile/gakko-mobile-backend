package com.gakkomobile.surveys.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitSurveyRequestDTO(
        @NotNull List<@Valid AnswerDTO> answers
) {
}
