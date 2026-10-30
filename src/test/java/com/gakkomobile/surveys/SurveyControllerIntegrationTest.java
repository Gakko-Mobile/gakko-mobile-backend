package com.gakkomobile.surveys;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gakkomobile.BaseIntegrationTest;
import com.gakkomobile.config.WithMockUser;
import com.gakkomobile.security.jwt.JwtService;
import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SurveyStatus;
import com.gakkomobile.surveys.dto.AdminSurveyDetailsDTO;
import com.gakkomobile.surveys.dto.AnswerDTO;
import com.gakkomobile.surveys.dto.CreateSurveyCommand;
import com.gakkomobile.surveys.dto.SubmitSurveyRequestDTO;
import com.gakkomobile.surveys.dto.SurveyQuestionDTO;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

class SurveyControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("Lifecycle and publishing")
    class LifecycleTests {

        @Test
        @WithMockUser(role = "EMPLOYEE")
        @DisplayName("Should create a draft and publish it with an outbox event")
        void publishSurvey_shouldCollectResponsesAndWriteOutboxEvent() {

            useEmployeeAccount();
            AdminSurveyDetailsDTO createdSurvey = createDraft();

            assertThat(createdSurvey.status()).isEqualTo(SurveyStatus.DRAFT);

            AdminSurveyDetailsDTO publishedSurvey = authenticatedClient.post()
                    .uri("/api/v1/admin/surveys/{surveyId}/publish", createdSurvey.id())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(AdminSurveyDetailsDTO.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(publishedSurvey).isNotNull();
            assertThat(publishedSurvey.status()).isEqualTo(SurveyStatus.COLLECTING);

            Long eventCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM outbox_events
                    WHERE aggregate_type = ? AND aggregate_id = ?
                    """, Long.class, "SURVEY", createdSurvey.id().toString());
            assertThat(eventCount).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Soft delete")
    class SoftDeleteTests {

        @Test
        @WithMockUser(role = "EMPLOYEE")
        @DisplayName("Should hide a deleted survey from pending feeds and keep the database row")
        void deleteSurvey_shouldHideSurveyAndKeepRow() {

            useEmployeeAccount();
            AdminSurveyDetailsDTO createdSurvey = createDraft();
            publishSurvey(createdSurvey.id());
            useStudentAccount();

            authenticatedClient.get()
                    .uri("/api/v1/surveys/pending")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content[?(@.id == '%s')]".formatted(createdSurvey.id())).isNotEmpty();

            useEmployeeAccount();
            authenticatedClient.delete()
                    .uri("/api/v1/admin/surveys/{surveyId}", createdSurvey.id())
                    .exchange()
                    .expectStatus().isNoContent();

            useStudentAccount();
            authenticatedClient.get()
                    .uri("/api/v1/surveys/pending")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content.length()").isEqualTo(0);

            Object deletedAt = jdbcTemplate.queryForObject(
                    "SELECT deleted_at FROM surveys WHERE id = ?",
                    Object.class,
                    createdSurvey.id()
            );
            Long remainingRows = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM surveys WHERE id = ?",
                    Long.class,
                    createdSurvey.id()
            );
            assertThat(deletedAt).isNotNull();
            assertThat(remainingRows).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Anonymization")
    class AnonymizationTests {

        @Test
        @WithMockUser(role = "STUDENT")
        @DisplayName("Should store the completion owner and leave the response user empty")
        void submitResponse_whenAnonymous_shouldSeparateCompletionFromResponse() {

            AdminSurveyDetailsDTO publishedSurvey = publishedAnonymousSurvey();
            useStudentAccount();
            submitAnswer(publishedSurvey.id(), publishedSurvey.questions().get(0).id());

            UUID completionUserIdentifier = jdbcTemplate.queryForObject(
                    "SELECT user_id FROM survey_completions WHERE survey_id = ?",
                    UUID.class,
                    publishedSurvey.id()
            );
            UUID responseUserIdentifier = jdbcTemplate.queryForObject(
                    "SELECT user_id FROM survey_responses WHERE survey_id = ?",
                    UUID.class,
                    publishedSurvey.id()
            );

            assertThat(completionUserIdentifier).isEqualTo(currentUser.getId());
            assertThat(responseUserIdentifier).isNull();
        }
    }

    @Nested
    @DisplayName("Duplicate submission")
    class DuplicateSubmissionTests {

        @Test
        @WithMockUser(role = "STUDENT")
        @DisplayName("Should reject a second response when resubmission is disabled")
        void submitResponse_whenAlreadySubmitted_shouldReturnConflict() {

            AdminSurveyDetailsDTO publishedSurvey = publishedAnonymousSurvey();
            useStudentAccount();
            String questionIdentifier = publishedSurvey.questions().get(0).id();
            submitAnswer(publishedSurvey.id(), questionIdentifier);

            authenticatedClient.post()
                    .uri("/api/v1/surveys/{surveyId}/responses", publishedSurvey.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(submission(questionIdentifier))
                    .exchange()
                    .expectStatus().isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("Role access")
    class RoleAccessTests {

        @Test
        @WithMockUser(role = "STUDENT")
        @DisplayName("Should forbid students from administrative survey endpoints")
        void createSurvey_whenStudent_shouldReturnForbidden() {

            useStudentAccount();

            authenticatedClient.post()
                    .uri("/api/v1/admin/surveys")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(draftCommand())
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    private AdminSurveyDetailsDTO publishedAnonymousSurvey() {

        useEmployeeAccount();
        AdminSurveyDetailsDTO createdSurvey = createDraft();
        publishSurvey(createdSurvey.id());
        return createdSurvey;
    }

    private AdminSurveyDetailsDTO createDraft() {

        AdminSurveyDetailsDTO createdSurvey = authenticatedClient.post()
                .uri("/api/v1/admin/surveys")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(draftCommand())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AdminSurveyDetailsDTO.class)
                .returnResult()
                .getResponseBody();
        assertThat(createdSurvey).isNotNull();
        assertThat(createdSurvey.questions()).isNotEmpty();
        return createdSurvey;
    }

    private void publishSurvey(UUID surveyIdentifier) {

        authenticatedClient.post()
                .uri("/api/v1/admin/surveys/{surveyId}/publish", surveyIdentifier)
                .exchange()
                .expectStatus().isOk();
    }

    private void submitAnswer(UUID surveyIdentifier, String questionIdentifier) {

        authenticatedClient.post()
                .uri("/api/v1/surveys/{surveyId}/responses", surveyIdentifier)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(submission(questionIdentifier))
                .exchange()
                .expectStatus().isNoContent();
    }

    private SubmitSurveyRequestDTO submission(String questionIdentifier) {

        return new SubmitSurveyRequestDTO(List.of(new AnswerDTO(questionIdentifier, "Too easy")));
    }

    private CreateSurveyCommand draftCommand() {

        return new CreateSurveyCommand(
                "Course feedback",
                "How was the course?",
                List.of(new SurveyQuestionDTO(
                        null,
                        QuestionType.SINGLE,
                        "Pace",
                        true,
                        List.of("Too easy", "Just right", "Too hard"),
                        null,
                        null,
                        null,
                        null,
                        List.of()
                )),
                AudienceTargetType.ALL,
                List.of(),
                true,
                false,
                true,
                false,
                false,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    private void useEmployeeAccount() {

        if (userRepository.findByEmail("employee@pjatk.edu.pl").isEmpty()) {
            UserEntity employee = createAndSaveUser(uuid(1), "employee@pjatk.edu.pl");
            employee.setRole(Role.EMPLOYEE);
            userRepository.save(employee);
        }
        when(jwtService.extractUsername(anyString())).thenReturn("employee@pjatk.edu.pl");
        when(jwtService.isTokenValid(anyString(), any())).thenReturn(true);
    }

    private void useStudentAccount() {

        when(jwtService.extractUsername(anyString())).thenReturn("student@pjatk.edu.pl");
        when(jwtService.isTokenValid(anyString(), any())).thenReturn(true);
    }
}
