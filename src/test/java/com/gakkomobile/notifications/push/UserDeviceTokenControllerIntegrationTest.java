package com.gakkomobile.notifications.push;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.gakkomobile.BaseIntegrationTest;
import com.gakkomobile.config.WithMockUser;
import com.gakkomobile.notifications.DevicePlatform;
import com.gakkomobile.notifications.push.dto.UserDeviceTokenRegisterRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

class UserDeviceTokenControllerIntegrationTest extends BaseIntegrationTest {

    private static final String URI = "/api/v1/notifications/tokens";
    private static final String TOKEN = "fcm-token-123";

    @Autowired
    private UserDeviceTokenRepository tokenRepository;


    @Nested
    @DisplayName("POST /api/v1/notifications/tokens")
    class RegisterTokenTests {

        @Test
        @WithMockUser
        @DisplayName("Should register a new token for the authenticated user and return 201")
        void registerToken_whenTokenIsNew_shouldCreateTokenForCurrentUser() {

            authenticatedClient.post()
                    .uri(URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerBody(TOKEN))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody().isEmpty();

            assertThat(tokenRepository.findAllByUserId(uuid(0)))
                    .hasSize(1)
                    .first()
                    .satisfies(entity -> assertThat(entity.getToken()).isEqualTo(TOKEN));
        }

        @Test
        @WithMockUser
        @DisplayName("Should reassign an existing token to the authenticated user instead of creating a duplicate")
        void registerToken_whenTokenBelongsToAnotherUser_shouldReassignToCurrentUser() {

            createAndSaveUser(uuid(1), "other@pjatk.edu.pl");
            createToken(uuid(1), TOKEN);

            authenticatedClient.post()
                    .uri(URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerBody(TOKEN))
                    .exchange()
                    .expectStatus().isCreated();

            assertThat(tokenRepository.findAll()).hasSize(1);
            assertThat(tokenRepository.findByToken(TOKEN).orElseThrow().getUserId()).isEqualTo(uuid(0));
            assertThat(tokenRepository.findAllByUserId(uuid(1))).isEmpty();
        }

        @Test
        @WithMockUser
        @DisplayName("Should be idempotent when the same user registers the same token twice")
        void registerToken_whenCalledTwiceWithSameToken_shouldNotCreateDuplicate() {

            for (int i = 0; i < 2; i++) {
                authenticatedClient.post()
                        .uri(URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(registerBody(TOKEN))
                        .exchange()
                        .expectStatus().isCreated();
            }

            assertThat(tokenRepository.findAllByUserId(uuid(0))).hasSize(1);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 Bad Request when fcmToken is blank")
        void registerToken_whenTokenIsBlank_shouldReturn400() {

            authenticatedClient.post()
                    .uri(URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerBody(""))
                    .exchange()
                    .expectStatus().isBadRequest();

            assertThat(tokenRepository.findAll()).isEmpty();
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 Bad Request when request body is missing")
        void registerToken_whenBodyIsMissing_shouldReturn400() {

            authenticatedClient.post()
                    .uri(URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Should reject unauthenticated requests")
        void registerToken_whenUnauthenticated_shouldReturn4xx() {

            unauthenticatedClient().post()
                    .uri(URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerBody(TOKEN))
                    .exchange()
                    .expectStatus().is4xxClientError();

            assertThat(tokenRepository.findAll()).isEmpty();
        }
    }


    @Nested
    @DisplayName("GET /api/v1/notifications/tokens")
    class GetMyDevicesTests {

        @Test
        @WithMockUser
        @DisplayName("Should return only the devices of the authenticated user")
        void getMyDevices_shouldReturnOnlyCurrentUserDevices() {

            createAndSaveUser(uuid(1), "other@pjatk.edu.pl");
            createToken(uuid(0), "token-mine-1");
            createToken(uuid(0), "token-mine-2");
            createToken(uuid(1), "token-other");

            authenticatedClient.get()
                    .uri(URI)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.length()").isEqualTo(2)
                    .jsonPath("$[?(@.fcmToken == 'token-other')]").isEmpty();
        }

        @Test
        @WithMockUser
        @DisplayName("Should return an empty list when the user has no registered devices")
        void getMyDevices_whenNoDevices_shouldReturnEmptyList() {

            authenticatedClient.get()
                    .uri(URI)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("Should reject unauthenticated requests")
        void getMyDevices_whenUnauthenticated_shouldReturn4xx() {

            unauthenticatedClient().get()
                    .uri(URI)
                    .exchange()
                    .expectStatus().is4xxClientError();
        }
    }


    @Nested
    @DisplayName("DELETE /api/v1/notifications/tokens/{fcmToken}")
    class UnregisterTokenTests {

        @Test
        @WithMockUser
        @DisplayName("Should delete the token of the authenticated user and return 204")
        void unregisterToken_whenTokenBelongsToUser_shouldDeleteIt() {

            createToken(uuid(0), TOKEN);

            authenticatedClient.delete()
                    .uri(URI + "/{token}", TOKEN)
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(tokenRepository.findByToken(TOKEN)).isEmpty();
        }

        @Test
        @WithMockUser
        @DisplayName("Should not delete a token that belongs to another user")
        void unregisterToken_whenTokenBelongsToAnotherUser_shouldNotDeleteIt() {

            createAndSaveUser(uuid(1), "other@pjatk.edu.pl");
            createToken(uuid(1), TOKEN);

            authenticatedClient.delete()
                    .uri(URI + "/{token}", TOKEN)
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(tokenRepository.findByToken(TOKEN)).isPresent();
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 204 when the token does not exist")
        void unregisterToken_whenTokenDoesNotExist_shouldReturn204() {

            authenticatedClient.delete()
                    .uri(URI + "/{token}", "does-not-exist")
                    .exchange()
                    .expectStatus().isNoContent();
        }

        @Test
        @DisplayName("Should reject unauthenticated requests")
        void unregisterToken_whenUnauthenticated_shouldReturn4xx() {

            createToken(uuid(0), TOKEN);

            unauthenticatedClient().delete()
                    .uri(URI + "/{token}", TOKEN)
                    .exchange()
                    .expectStatus().is4xxClientError();

            assertThat(tokenRepository.findByToken(TOKEN)).isPresent();
        }
    }


    private WebTestClient unauthenticatedClient() {

        return authenticatedClient.mutate()
                .defaultHeaders(headers -> headers.remove("Authorization"))
                .build();
    }

    private UserDeviceTokenRegisterRequestDTO registerBody(String fcmToken) {

        return UserDeviceTokenRegisterRequestDTO.builder()
                .fcmToken(fcmToken)
                .platform(DevicePlatform.ANDROID.toString())
                .deviceName("Example device")
                .build();
    }

    private void createToken(UUID userId, String token) {

        UserDeviceTokenEntity entity = new UserDeviceTokenEntity();
        entity.setUserId(userId);
        entity.setToken(token);
        entity.setPlatform(DevicePlatform.ANDROID);
        persistAndCommit(entity);
    }
}