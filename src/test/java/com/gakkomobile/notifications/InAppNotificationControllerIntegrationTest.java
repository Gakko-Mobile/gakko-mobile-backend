package com.gakkomobile.notifications;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.gakkomobile.BaseIntegrationTest;
import com.gakkomobile.config.WithMockUser;
import com.gakkomobile.user.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.UUID;

class InAppNotificationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private InAppNotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {

        notificationRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetNotificationsTests {

        @Test
        @WithMockUser
        @DisplayName("Should return a paginated list of notifications for the authenticated user")
        void getNotifications_shouldReturnPaginatedNotificationsForCurrentUser() {

            // given
            createAndSaveNotification(currentUser.getId(), "Title 1", "Body 1", false);
            createAndSaveNotification(currentUser.getId(), "Title 2", "Body 2", true);

            UserEntity anotherUser = createAndSaveUser(uuid(1), "other@gakko.edu.pl");
            createAndSaveNotification(anotherUser.getId(), "Other Title", "Other Body", false);

            // when & then
            authenticatedClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/notifications")
                            .queryParam("page", "0")
                            .queryParam("size", "10")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(2)
                    .jsonPath("$.totalElements").isEqualTo(2)
                    .jsonPath("$.content[0].title").isEqualTo("Title 2")
                    .jsonPath("$.content[1].title").isEqualTo("Title 1");
        }

        @Test
        @WithMockUser
        @DisplayName("Should return an empty page when the user has no notifications")
        void getNotifications_whenUserHasNoNotifications_shouldReturnEmptyPage() {

            authenticatedClient.get()
                    .uri("/api/v1/notifications")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(0)
                    .jsonPath("$.totalElements").isEqualTo(0)
                    .jsonPath("$.empty").isEqualTo(true);
        }

        @Test
        @WithMockUser
        @DisplayName("Should respect pagination size parameters when page limit is smaller than total items")
        void getNotifications_shouldRespectPaginationSize() {

            // given
            createAndSaveNotification(currentUser.getId(), "Title 1", "Body 1", false);
            createAndSaveNotification(currentUser.getId(), "Title 2", "Body 2", false);
            createAndSaveNotification(currentUser.getId(), "Title 3", "Body 3", false);

            // when & then
            authenticatedClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/notifications")
                            .queryParam("page", "0")
                            .queryParam("size", "2")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(2)
                    .jsonPath("$.totalElements").isEqualTo(3)
                    .jsonPath("$.totalPages").isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/{notificationId}")
    class GetNotificationByIdTests {

        @Test
        @WithMockUser
        @DisplayName("Should return notification details when notification exists and belongs to the user")
        void getNotificationById_shouldReturnNotificationWhenExists() {

            // given
            InAppNotificationEntity savedNotification = createAndSaveNotification(currentUser.getId(), "Grade", "Grade 5.0 released", false);

            // when & then
            authenticatedClient.get()
                    .uri("/api/v1/notifications/{notificationId}", savedNotification.getId())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(savedNotification.getId().toString())
                    .jsonPath("$.title").isEqualTo("Grade")
                    .jsonPath("$.body").isEqualTo("Grade 5.0 released");
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 Not Found when notification belongs to another user")
        void getNotificationById_shouldReturn404WhenNotificationBelongsToAnotherUser() {

            // given
            UserEntity anotherUser = createAndSaveUser(uuid(1), "other@pjatk.edu.pl");
            InAppNotificationEntity otherNotification = createAndSaveNotification(anotherUser.getId(), "Other Title", "Other Content", false);

            // when & then
            authenticatedClient.get()
                    .uri("/api/v1/notifications/{notificationId}", otherNotification.getId())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 Not Found when notification ID does not exist")
        void getNotificationById_shouldReturn404WhenNotificationDoesNotExist() {

            // given
            UUID nonExistentNotificationId = UUID.randomUUID();

            // when & then
            authenticatedClient.get()
                    .uri("/api/v1/notifications/{notificationId}", nonExistentNotificationId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 Bad Request when notification ID format is invalid")
        void getNotificationById_shouldReturn400WhenNotificationIdIsInvalidUuid() {

            authenticatedClient.get()
                    .uri("/api/v1/notifications/{notificationId}", "invalid-uuid-string")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/{notificationId}")
    class ToggleNotificationReadStatusTests {

        @Test
        @WithMockUser
        @DisplayName("Should toggle notification read status from unread to read")
        void toggleNotificationReadStatus_shouldToggleUnreadToRead() {

            // given
            InAppNotificationEntity notification = createAndSaveNotification(currentUser.getId(), "Test Title", "Test body", false);

            // when & then
            authenticatedClient.patch()
                    .uri("/api/v1/notifications/{notificationId}", notification.getId())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(notification.getId().toString())
                    .jsonPath("$.isRead").isEqualTo(true);

            InAppNotificationEntity updatedNotification = notificationRepository.findById(notification.getId()).orElseThrow();
            assertThat(updatedNotification.isRead()).isTrue();
        }

        @Test
        @WithMockUser
        @DisplayName("Should toggle notification read status from read to unread")
        void toggleNotificationReadStatus_shouldToggleReadToUnread() {

            // given
            InAppNotificationEntity notification = createAndSaveNotification(currentUser.getId(), "Test Title", "Test body", true);

            // when & then
            authenticatedClient.patch()
                    .uri("/api/v1/notifications/{notificationId}", notification.getId())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(notification.getId().toString())
                    .jsonPath("$.isRead").isEqualTo(false);

            InAppNotificationEntity updatedNotification = notificationRepository.findById(notification.getId()).orElseThrow();
            assertThat(updatedNotification.isRead()).isFalse();
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 Not Found when toggling status of non-existent notification")
        void toggleNotificationReadStatus_shouldReturn404WhenNotificationDoesNotExist() {

            UUID nonExistentId = UUID.randomUUID();

            authenticatedClient.patch()
                    .uri("/api/v1/notifications/{notificationId}", nonExistentId)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/read-status")
    class MarkAllNotificationsReadStatusTests {

        @Test
        @WithMockUser
        @DisplayName("Should mark all user notifications as read and return 204 No Content")
        void markAllNotificationsReadStatus_shouldMarkAllAsRead() {

            // given
            createAndSaveNotification(currentUser.getId(), "N1", "B1", false);
            createAndSaveNotification(currentUser.getId(), "N2", "B2", false);

            // when & then
            authenticatedClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/notifications/read-status")
                            .queryParam("isRead", "true")
                            .build())
                    .exchange()
                    .expectStatus().isNoContent();

            long unreadCount = notificationRepository.findAllByUserId(currentUser.getId(), null)
                    .stream()
                    .filter(n -> !n.isRead())
                    .count();

            assertThat(unreadCount).isZero();
        }

        @Test
        @WithMockUser
        @DisplayName("Should mark all user notifications as unread when isRead query parameter is false")
        void markAllNotificationsReadStatus_shouldMarkAllAsUnread() {

            // given
            createAndSaveNotification(currentUser.getId(), "N1", "B1", true);
            createAndSaveNotification(currentUser.getId(), "N2", "B2", true);

            // when & then
            authenticatedClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/notifications/read-status")
                            .queryParam("isRead", "false")
                            .build())
                    .exchange()
                    .expectStatus().isNoContent();

            long readCount = notificationRepository.findAllByUserId(currentUser.getId(), null)
                    .stream()
                    .filter(InAppNotificationEntity::isRead)
                    .count();

            assertThat(readCount).isZero();
        }

        @Test
        @WithMockUser
        @DisplayName("Should not affect notifications belonging to other users when performing bulk update")
        void markAllNotificationsReadStatus_shouldNotAffectOtherUsers() {

            // given
            UserEntity anotherUser = createAndSaveUser(uuid(1), "other@pjatk.edu.pl");
            createAndSaveNotification(currentUser.getId(), "Mine", "Body", false);
            InAppNotificationEntity otherNotification = createAndSaveNotification(anotherUser.getId(), "Other", "Body", false);

            // when
            authenticatedClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/notifications/read-status")
                            .queryParam("isRead", "true")
                            .build())
                    .exchange()
                    .expectStatus().isNoContent();

            // then
            InAppNotificationEntity untouchedOther = notificationRepository.findById(otherNotification.getId()).orElseThrow();
            assertThat(untouchedOther.isRead()).isFalse();
        }
    }

    private InAppNotificationEntity createAndSaveNotification(UUID userId, String title, String body, boolean isRead) {

        InAppNotificationEntity notification = new InAppNotificationEntity();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setCategory(NotificationCategory.ACADEMIC_GRADES.toString());
        notification.setRead(isRead);
        notification.setCreatedAt(Instant.now());
        return notificationRepository.saveAndFlush(notification);
    }
}