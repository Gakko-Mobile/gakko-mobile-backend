package com.gakkomobile.surveys.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gakkomobile.surveys.domain.AudienceTargetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record CreateSurveyCommand(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull @NotEmpty @Valid List<SurveyQuestionDTO> questions,
        AudienceTargetType targetType,
        List<String> targetIds,
        @JsonProperty("isAnonymous") Boolean anonymous,
        Boolean allowResubmission,
        Boolean requireLogin,
        Boolean showResultsToStudents,
        Boolean randomizeQuestionOrder,
        @Positive Integer responseLimit,
        String confirmationMessage,
        OffsetDateTime opensAt,
        OffsetDateTime closesAt,
        List<@Positive Double> reminderIntervals
) {
}
