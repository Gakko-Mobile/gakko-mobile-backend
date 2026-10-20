package com.gakkomobile.notifications.push.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserDeviceTokenRegisterRequestDTO(

        @NotBlank(message = "FCM token cannot be blank")
        @Size(max = 500, message = "FCM token is too long")
        String fcmToken,

        @NotBlank(message = "Platform cannot be blank")
        @Pattern(regexp = "^(ANDROID|IOS)$", message = "Platform must be either ANDROID or IOS")
        String platform,

        @Size(max = 100, message = "Device name cannot exceed 100 characters")
        String deviceName
) {
}
