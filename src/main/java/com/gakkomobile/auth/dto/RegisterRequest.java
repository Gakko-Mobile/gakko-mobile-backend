package com.gakkomobile.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String profilePhoto,

        @NotBlank
        @Pattern(regexp = "^s\\d{5}$", message = "Index must follow the pattern sXXXXX")
        String index,

        @NotBlank
        @Pattern(regexp = "^\\d{26}$", message = "Bank account must contain exactly 26 digits")
        String individualBankAccount,

        @NotBlank
        @Email(message = "Must be a well-formed email address")
        String email,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\p{Punct})[a-zA-Z0-9\\p{Punct}]{8,}$",
                message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one digit, one special character, and use only Latin letters."
        )
        String password
) {}

