package com.gakkomobile.surveys.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerDTO(
        @NotBlank String questionId,
        Object answerValue
) {
}
