package com.gakkomobile.surveys.domain;

import java.util.List;

public record SurveyQuestion(
        String id,
        QuestionType type,
        String title,
        boolean isRequired,
        List<String> options,
        Integer scaleMin,
        Integer scaleMax,
        String labelMin,
        String labelMax,
        List<SkipRule> skipRules
) {
}
