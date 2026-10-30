package com.gakkomobile.surveys.dto;

import lombok.Builder;

@Builder
public record TextAnswerGroupDTO(
        String value,
        long count
) {
}
