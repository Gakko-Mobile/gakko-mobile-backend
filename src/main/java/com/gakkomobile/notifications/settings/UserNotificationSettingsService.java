package com.gakkomobile.notifications.settings;

import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.notifications.NotificationCategory;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.NotificationMode;
import com.gakkomobile.notifications.settings.dto.UpdateNotificationSettingsRequestDTO;
import com.gakkomobile.notifications.settings.dto.UserNotificationSettingsDTO;
import com.gakkomobile.notifications.settings.dto.UserNotificationSettingsResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationSettingsService {

    private final UserNotificationSettingsRepository settingsRepository;
    private final UserNotificationSettingsMapper settingsMapper;

    public UserNotificationSettingsResponseDTO getUserNotificationsSettings(UUID userId) {

        return settingsRepository.findById(userId)
                .map(settingsMapper::entityToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User notification settings not found for user: " + userId));
    }

    public NotificationMode getUserNotificationSettingsMode(NotificationDTO notificationDTO, UUID targetId) {

        UserNotificationSettingsDTO settings = settingsRepository.findById(targetId)
                .map(settingsMapper::entityToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User notification settings not found for user: " + targetId));

        return resolveMode(settings, notificationDTO.category());
    }

    private NotificationMode resolveMode(UserNotificationSettingsDTO settings, String category) {

        NotificationCategory notificationCategory;
        try {
            notificationCategory = NotificationCategory.valueOf(category);
        } catch (IllegalArgumentException | NullPointerException e) {

            log.warn("Invalid notification category: {}", category);
            return NotificationMode.OFF;
        }

        return switch (notificationCategory) {
            case ACADEMIC_GRADES -> settings.academicGrades();
            case ACADEMIC_SCHEDULE_CHANGES -> settings.academicScheduleChanges();
            case ACADEMIC_CANCELLED_CLASSES -> settings.academicCancelledClasses();
            case FINANCIAL_NEW_CHARGES -> settings.financialNewCharges();
            case EVENTS_STUDENT -> settings.eventsStudent();
        };
    }

    @Transactional
    public void updateUserNotificationSettings(UUID userId, UpdateNotificationSettingsRequestDTO request) {

        UserNotificationSettingsEntity settings = settingsRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User notification settings not found for user: " + userId));

        settings.setAcademicGrades(request.academicGrades());
        settings.setAcademicScheduleChanges(request.academicScheduleChanges());
        settings.setAcademicCancelledClasses(request.academicCancelledClasses());
        settings.setFinancialNewCharges(request.financialNewCharges());
        settings.setEventsStudent(request.eventsStudent());
    }
}
