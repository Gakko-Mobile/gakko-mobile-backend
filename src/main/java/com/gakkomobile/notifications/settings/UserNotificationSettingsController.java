package com.gakkomobile.notifications.settings;

import com.gakkomobile.notifications.settings.dto.UpdateNotificationSettingsRequestDTO;
import com.gakkomobile.notifications.settings.dto.UserNotificationSettingsResponseDTO;
import com.gakkomobile.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/notifications/settings")
@RequiredArgsConstructor
public class UserNotificationSettingsController {

    private final UserNotificationSettingsService userNotificationSettingsService;

    @GetMapping
    public ResponseEntity<UserNotificationSettingsResponseDTO> getUserNotificationSettings(@AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(userNotificationSettingsService.getUserNotificationsSettings(currentUser.getId()));
    }

    @PatchMapping
    public ResponseEntity<Void> updateNotificationSettings(@AuthenticationPrincipal User currentUser,
                                                           @RequestBody @Valid UpdateNotificationSettingsRequestDTO request) {

        userNotificationSettingsService.updateUserNotificationSettings(currentUser.getId(), request);
        return ResponseEntity.ok().build();
    }
}
