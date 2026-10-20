package com.gakkomobile.notifications.settings;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.gakkomobile.BaseIntegrationTest;
import com.gakkomobile.config.WithMockUser;
import com.gakkomobile.notifications.NotificationMode;
import com.gakkomobile.notifications.settings.dto.UpdateNotificationSettingsRequestDTO;
import com.gakkomobile.user.UserEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

class UserNotificationSettingsControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserNotificationSettingsRepository settingsRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        settingsRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/settings")
    class GetUserNotificationSettingsTests {

        @Test
        @WithMockUser
        @DisplayName("Should return notification settings when user settings exist")
        void getUserNotificationSettings_whenSettingsExist_shouldReturnSettings() {

            // given
            createAndSaveDefaultSettings(uuid(0));

            // when & then
            authenticatedClient.get()
                    .uri("/api/v1/notifications/settings")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.academicGrades").isEqualTo("PUSH_AND_INAPP")
                    .jsonPath("$.academicScheduleChanges").isEqualTo("PUSH_AND_INAPP")
                    .jsonPath("$.academicCancelledClasses").isEqualTo("PUSH_AND_INAPP")
                    .jsonPath("$.financialNewCharges").isEqualTo("PUSH_AND_INAPP")
                    .jsonPath("$.eventsStudent").isEqualTo("INAPP_ONLY");
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 Not Found when settings do not exist for the authenticated user")
        void getUserNotificationSettings_whenSettingsNotFound_shouldReturn404() {

            authenticatedClient.get()
                    .uri("/api/v1/notifications/settings")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @WithMockUser
        @DisplayName("Should ensure settings isolation and return correct preferences for authenticated user")
        void getUserNotificationSettings_shouldEnsureDataIsolationBetweenUsers() {

            // given
            createAndSaveDefaultSettings(uuid(0));

            UserEntity otherUser = createAndSaveUser(uuid(1), "other@pjatk.edu.pl");
            UserNotificationSettingsEntity otherSettings = new UserNotificationSettingsEntity();
            otherSettings.setUserId(otherUser.getId());
            otherSettings.setAcademicGrades(NotificationMode.OFF);
            otherSettings.setEventsStudent(NotificationMode.OFF);
            persistAndCommit(otherSettings);

            // when & then
            authenticatedClient.get()
                    .uri("/api/v1/notifications/settings")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.academicGrades").isEqualTo("PUSH_AND_INAPP")
                    .jsonPath("$.eventsStudent").isEqualTo("INAPP_ONLY");
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/settings")
    class UpdateNotificationSettingsTests {

        @Test
        @WithMockUser
        @DisplayName("Should update all notification settings when request payload is valid")
        void updateNotificationSettings_whenRequestIsValid_shouldUpdateSettingsInDatabase() {

            // given
            createAndSaveDefaultSettings(uuid(0));

            UpdateNotificationSettingsRequestDTO updateRequest = new UpdateNotificationSettingsRequestDTO(
                    NotificationMode.OFF,
                    NotificationMode.INAPP_ONLY,
                    NotificationMode.OFF,
                    NotificationMode.PUSH_AND_INAPP,
                    NotificationMode.OFF
            );

            // when & then
            authenticatedClient.patch()
                    .uri("/api/v1/notifications/settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().isOk();

            entityManager.clear();

            UserNotificationSettingsEntity updatedEntity = settingsRepository.findById(uuid(0)).orElseThrow();
            assertThat(updatedEntity.getAcademicGrades()).isEqualTo(NotificationMode.OFF);
            assertThat(updatedEntity.getAcademicScheduleChanges()).isEqualTo(NotificationMode.INAPP_ONLY);
            assertThat(updatedEntity.getAcademicCancelledClasses()).isEqualTo(NotificationMode.OFF);
            assertThat(updatedEntity.getFinancialNewCharges()).isEqualTo(NotificationMode.PUSH_AND_INAPP);
            assertThat(updatedEntity.getEventsStudent()).isEqualTo(NotificationMode.OFF);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 Not Found when updating settings for non-existent user settings record")
        void updateNotificationSettings_whenSettingsRecordDoesNotExist_shouldReturn404() {

            // given
            UpdateNotificationSettingsRequestDTO updateRequest = new UpdateNotificationSettingsRequestDTO(
                    NotificationMode.OFF,
                    NotificationMode.OFF,
                    NotificationMode.OFF,
                    NotificationMode.OFF,
                    NotificationMode.OFF
            );

            // when & then
            authenticatedClient.patch()
                    .uri("/api/v1/notifications/settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 Bad Request when request body contains null values for required fields")
        void updateNotificationSettings_whenRequestBodyHasNullFields_shouldReturn400() {

            // given
            createAndSaveDefaultSettings(uuid(0));

            UpdateNotificationSettingsRequestDTO invalidRequest = new UpdateNotificationSettingsRequestDTO(
                    null,
                    NotificationMode.INAPP_ONLY,
                    NotificationMode.OFF,
                    NotificationMode.PUSH_AND_INAPP,
                    NotificationMode.OFF
            );

            // when & then
            authenticatedClient.patch()
                    .uri("/api/v1/notifications/settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidRequest)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 Bad Request when request payload contains invalid NotificationMode string")
        void updateNotificationSettings_whenEnumModeIsInvalid_shouldReturn400() {

            // given
            createAndSaveDefaultSettings(uuid(0));

            String rawInvalidJsonBody = """
                    {
                        "academicGrades": "INVALID_MODE",
                        "academicScheduleChanges": "PUSH_AND_INAPP",
                        "academicCancelledClasses": "PUSH_AND_INAPP",
                        "financialNewCharges": "PUSH_AND_INAPP",
                        "eventsStudent": "INAPP_ONLY"
                    }
                    """;

            // when & then
            authenticatedClient.patch()
                    .uri("/api/v1/notifications/settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(rawInvalidJsonBody)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    private void createAndSaveDefaultSettings(UUID userId) {

        UserNotificationSettingsEntity settings = new UserNotificationSettingsEntity();
        settings.setUserId(userId);
        settings.setAcademicGrades(NotificationMode.PUSH_AND_INAPP);
        settings.setAcademicScheduleChanges(NotificationMode.PUSH_AND_INAPP);
        settings.setAcademicCancelledClasses(NotificationMode.PUSH_AND_INAPP);
        settings.setFinancialNewCharges(NotificationMode.PUSH_AND_INAPP);
        settings.setEventsStudent(NotificationMode.INAPP_ONLY);

        persistAndCommit(settings);
    }
}