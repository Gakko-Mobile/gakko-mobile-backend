package com.gakkomobile.surveys.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gakkomobile.surveys.domain.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SurveyQuestionDTO(
        String id,
        @NotNull QuestionType type,
        @NotBlank String title,
        @JsonProperty("isRequired") boolean isRequired,
        List<String> options,
        Integer scaleMin,
        Integer scaleMax,
        String labelMin,
        String labelMax,
        @Valid List<SkipLogicDTO> skipRules
) {
}
