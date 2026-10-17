package com.gakkomobile.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDTO(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
