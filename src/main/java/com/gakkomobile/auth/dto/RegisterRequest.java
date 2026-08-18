package com.gakkomobile.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Pattern(
                regexp = "^s\\d{3,}$",
                message = "Index number must start with 's' followed by at least 3 digits"
        )
        String indexNumber,

        @NotBlank
        @Pattern(regexp = "^\\d{11}$", message = "PESEL must contain exactly 11 digits")
        String pesel,

//        @Pattern(regexp = "^\\d{26}$", message = "Bank account must contain exactly 26 digits")
//        String individualBankAccount,

        @NotBlank
        @Email(message = "Must be a well-formed email address")
        @Size(max = 255)
        String email,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\p{Punct})[a-zA-Z0-9\\p{Punct}]{8,}$",
                message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one digit, one special character, and use only Latin letters."
        )
        String password
) {}

