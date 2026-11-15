package com.gakkomobile.messages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessagePayloadDTO(
        @NotBlank
        @Size(max = 4000)
        String content,
        boolean isUrgent
) {
}
