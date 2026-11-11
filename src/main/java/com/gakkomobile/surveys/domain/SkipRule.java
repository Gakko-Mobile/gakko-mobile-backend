package com.gakkomobile.surveys.domain;

public record SkipRule(
        String triggerOptionId,
        String jumpToQuestionId
) {
}
