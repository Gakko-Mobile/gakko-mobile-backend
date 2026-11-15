package com.gakkomobile.messages.dto;

import com.gakkomobile.messages.domain.ThreadType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateThreadRequestDTO(
        @NotBlank
        @Size(max = 255)
        String subject,
        @NotNull
        ThreadType threadType,
        @NotNull
        List<@NotNull UUID> participantIds
) {
}
