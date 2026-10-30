package com.gakkomobile.surveys.dto;

import com.gakkomobile.surveys.domain.QuestionType;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record QuestionStatisticsDTO(
        String questionId,
        QuestionType type,
        String title,
        Map<String, Long> distribution,
        List<TextAnswerGroupDTO> textResponses
) {
}
