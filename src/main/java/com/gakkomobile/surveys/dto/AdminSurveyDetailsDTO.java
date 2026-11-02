package com.gakkomobile.surveys.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record AdminSurveyDetailsDTO(
        UUID id,
        UUID creatorId,
        String authorName,
        String title,
        String description,
        List<SurveyQuestionDTO> questions,
        SurveyStatus status,
        AudienceTargetType targetType,
        List<String> targetIds,
        @JsonProperty("isAnonymous") boolean anonymous,
        boolean allowResubmission,
        boolean requireLogin,
        boolean showResultsToStudents,
        boolean randomizeQuestionOrder,
        Integer responseLimit,
        String confirmationMessage,
        OffsetDateTime opensAt,
        OffsetDateTime closesAt,
        List<Double> reminderIntervals,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
