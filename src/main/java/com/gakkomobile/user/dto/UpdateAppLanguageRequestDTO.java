package com.gakkomobile.user.dto;

import com.gakkomobile.user.AppLanguage;
import jakarta.validation.constraints.NotNull;

public record UpdateAppLanguageRequestDTO(
        @NotNull
        AppLanguage language
) {
}
