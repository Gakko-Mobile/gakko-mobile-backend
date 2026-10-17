package com.gakkomobile.notifications;

import com.gakkomobile.notifications.dto.InAppNotificationReadInfoResponseDTO;
import com.gakkomobile.notifications.dto.InAppNotificationResponseDTO;
import com.gakkomobile.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
@RequestMapping("api/v1/notifications")
@RequiredArgsConstructor
public class InAppNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<InAppNotificationResponseDTO>> getNotifications(
            @AuthenticationPrincipal UserEntity currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<InAppNotificationResponseDTO> notifications = notificationService.getNotifications(currentUser.getId(), pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<InAppNotificationResponseDTO> getNotificationById(@AuthenticationPrincipal UserEntity currentUser,
                                                                            @PathVariable UUID notificationId) {

        return ResponseEntity.ok(notificationService.getNotificationById(notificationId, currentUser.getId()));
    }

    @PatchMapping("/{notificationId}")
    public ResponseEntity<InAppNotificationReadInfoResponseDTO> toggleNotificationReadStatus(@AuthenticationPrincipal UserEntity currentUser,
                                                                                             @PathVariable UUID notificationId) {

        return ResponseEntity.ok(notificationService.markNotificationReadStatus(notificationId, currentUser.getId()));
    }

    @PatchMapping("/read-status")
    public ResponseEntity<Void> markAllNotificationsReadStatus(@AuthenticationPrincipal UserEntity currentUser,
                                                               @RequestParam(defaultValue = "true") boolean isRead) {

        notificationService.markAllNotificationsReadStatus(currentUser.getId(), isRead);
        return ResponseEntity.noContent().build();
    }
}
