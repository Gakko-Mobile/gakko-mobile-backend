package com.gakkomobile.surveys.dto;

import jakarta.validation.constraints.NotBlank;

public record SkipLogicDTO(
        @NotBlank String triggerOptionId,
        @NotBlank String jumpToQuestionId
) {
}
