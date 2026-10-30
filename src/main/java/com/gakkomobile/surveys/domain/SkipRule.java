package com.gakkomobile.surveys.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SkipRule(
        @JsonProperty("triggerOptionId") String triggerOptionId,
        @JsonProperty("jumpToQuestionId") String jumpToQuestionId
) {
}
