package com.gakkomobile.surveys.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record SurveyAnalyticsDTO(
        UUID surveyId,
        long responseCount,
        Long audienceSize,
        Integer responseLimit,
        double completionRatio,
        List<QuestionStatisticsDTO> questions
) {
}
