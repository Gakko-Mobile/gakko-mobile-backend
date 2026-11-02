package com.gakkomobile.surveys.dto;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record SurveyResultDTO(
        UUID surveyId,
        long responseCount,
        double completionRatio,
        List<QuestionStatisticsDTO> questions
) {
}
