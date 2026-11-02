package com.gakkomobile.surveys.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gakkomobile.surveys.domain.SurveyStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record SurveyResponseDTO(
        UUID id,
        String title,
        String description,
        String authorName,
        OffsetDateTime deadline,
        int questionCount,
        String privacyStatus,
        String estimatedTimeLeft,
        @JsonProperty("isNew") boolean isNew,
        @JsonProperty("isCompleted") boolean isCompleted,
        SurveyStatus status,
        @JsonProperty("showResultsToStudents") boolean showResultsToStudents,
        boolean allowResubmission,
        boolean requireLogin,
        String confirmationMessage,
        List<SurveyQuestionDTO> questions
) {
}
