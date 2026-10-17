package com.gakkomobile.auth.dto;

public record AuthResponseDTO(
        String accessToken,
        String refreshToken
) {}
