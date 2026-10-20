package com.gakkomobile.notifications.settings;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.notifications.NotificationCategory;
import com.gakkomobile.notifications.NotificationMode;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.settings.dto.UpdateNotificationSettingsRequestDTO;
import com.gakkomobile.notifications.settings.dto.UserNotificationSettingsDTO;
import com.gakkomobile.notifications.settings.dto.UserNotificationSettingsResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UserNotificationSettingsServiceTest {

    @Mock
    private UserNotificationSettingsRepository settingsRepository;

    @Mock
    private UserNotificationSettingsMapper settingsMapper;

    @InjectMocks
    private UserNotificationSettingsService service;


    @Test
    void getUserNotificationsSettings_shouldReturnMappedResponse_whenSettingsFound() {

        // given
        UserNotificationSettingsEntity entity = new UserNotificationSettingsEntity();
        when(settingsRepository.findById(uuid(0))).thenReturn(Optional.of(entity));
        when(settingsMapper.entityToResponseDTO(entity)).thenReturn(UserNotificationSettingsResponseDTO.builder().build());

        // when
        UserNotificationSettingsResponseDTO response = service.getUserNotificationsSettings(uuid(0));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    void getUserNotificationsSettings_shouldThrowException_whenSettingsNotFound() {

        // given
        when(settingsRepository.findById(uuid(0))).thenReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () -> service.getUserNotificationsSettings(uuid(0)));
    }

    @Test
    void getUserNotificationSettingsMode_shouldReturnNotificationMode_whenSettingsNotFound() {

        // given
        NotificationDTO notificationDTO = NotificationDTO.builder().build();
        when(settingsRepository.findById(uuid(0))).thenReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () -> service.getUserNotificationSettingsMode(notificationDTO, uuid(0)));
    }

    @Test
    void getUserNotificationSettingsMode_shouldReturnOffMode_whenCategoryIsInvalid() {

        // given
        NotificationDTO notificationDTO = NotificationDTO.builder().build();
        UserNotificationSettingsEntity entity = new UserNotificationSettingsEntity();
        UserNotificationSettingsDTO userSettingsDTO = UserNotificationSettingsDTO.builder().build();

        when(settingsRepository.findById(uuid(0))).thenReturn(Optional.of(entity));
        when(settingsMapper.entityToDTO(entity)).thenReturn(userSettingsDTO);
        
        // when
        NotificationMode notificationMode = service.getUserNotificationSettingsMode(notificationDTO, uuid(0));

        // then
        assertThat(notificationMode).isNotNull();
        assertThat(notificationMode).isEqualTo(NotificationMode.OFF);
    }

    @ParameterizedTest
    @MethodSource("settingsModeProvider")
    void getUserNotificationSettingsMode_shouldReturnNotificationMode_whenSettingsFound(NotificationCategory category, NotificationMode mode) {

        // given
        NotificationDTO notificationDTO = NotificationDTO.builder().category(category.name()).build();
        UserNotificationSettingsEntity entity = new UserNotificationSettingsEntity();
        UserNotificationSettingsDTO userSettingsDTO = UserNotificationSettingsDTO.builder()
                .academicGrades(mode)
                .academicScheduleChanges(mode)
                .academicCancelledClasses(mode)
                .financialNewCharges(mode)
                .eventsStudent(mode)
                .build();

        when(settingsRepository.findById(uuid(0))).thenReturn(Optional.of(entity));
        when(settingsMapper.entityToDTO(entity)).thenReturn(userSettingsDTO);

        // when
        NotificationMode notificationMode = service.getUserNotificationSettingsMode(notificationDTO, uuid(0));

        // then
        assertThat(notificationMode).isNotNull();
        assertThat(notificationMode).isEqualTo(mode);
    }

    private static Stream<Arguments> settingsModeProvider() {
        return Stream.of(NotificationCategory.values())
                .flatMap(category -> Stream.of(NotificationMode.values())
                        .map(mode -> Arguments.of(category, mode)));
    }

    @Test
    void updateUserNotificationSettings_shouldUpdateSettings_whenSettingsFound() {

        // given
        UpdateNotificationSettingsRequestDTO request = UpdateNotificationSettingsRequestDTO.builder()
                .academicScheduleChanges(NotificationMode.PUSH_AND_INAPP)
                .academicCancelledClasses(NotificationMode.INAPP_ONLY)
                .financialNewCharges(NotificationMode.OFF)
                .eventsStudent(NotificationMode.PUSH_AND_INAPP)
                .academicGrades(NotificationMode.PUSH_AND_INAPP)
                .build();
        UserNotificationSettingsEntity entity = new UserNotificationSettingsEntity();
        when(settingsRepository.findById(uuid(0))).thenReturn(Optional.of(entity));

        // when
        service.updateUserNotificationSettings(uuid(0), request);

        // then
        assertThat(entity.getAcademicScheduleChanges()).isEqualTo(NotificationMode.PUSH_AND_INAPP);
        assertThat(entity.getAcademicCancelledClasses()).isEqualTo(NotificationMode.INAPP_ONLY);
        assertThat(entity.getFinancialNewCharges()).isEqualTo(NotificationMode.OFF);
        assertThat(entity.getEventsStudent()).isEqualTo(NotificationMode.PUSH_AND_INAPP);
        assertThat(entity.getAcademicGrades()).isEqualTo(NotificationMode.PUSH_AND_INAPP);
    }

    @Test
    void updateUserNotificationSettings_shouldThrowException_whenSettingsNotFound() {

        // given
        UpdateNotificationSettingsRequestDTO request = UpdateNotificationSettingsRequestDTO.builder()
                .academicScheduleChanges(NotificationMode.PUSH_AND_INAPP)
                .academicCancelledClasses(NotificationMode.INAPP_ONLY)
                .financialNewCharges(NotificationMode.OFF)
                .eventsStudent(NotificationMode.PUSH_AND_INAPP)
                .academicGrades(NotificationMode.PUSH_AND_INAPP)
                .build();
        when(settingsRepository.findById(uuid(0))).thenReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () -> service.updateUserNotificationSettings(uuid(0), request));
    }
}
