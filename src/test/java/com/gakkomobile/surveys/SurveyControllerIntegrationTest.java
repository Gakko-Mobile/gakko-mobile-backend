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
import com.gakkomobile.surveys.dto.SkipLogicDTO;
import com.gakkomobile.surveys.dto.SubmitSurveyRequestDTO;
import com.gakkomobile.surveys.dto.SurveyQuestionDTO;
import com.gakkomobile.surveys.dto.UpdateSurveyCommand;
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
        @WithMockUser()
        @DisplayName("Should store the completion owner and leave the response user empty")
        void submitResponse_whenAnonymous_shouldSeparateCompletionFromResponse() {

            AdminSurveyDetailsDTO publishedSurvey = publishedAnonymousSurvey();
            useStudentAccount();
            submitAnswer(publishedSurvey.id(), publishedSurvey.questions().getFirst().id());

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
    @DisplayName("Draft updates and manual close")
    class StaffMutationTests {

        @Test
        @WithMockUser(role = "EMPLOYEE")
        @DisplayName("Should update a draft and reject changes after publication")
        void updateSurvey_shouldAllowDraftsAndRejectCollectingSurveys() {

            useEmployeeAccount();
            AdminSurveyDetailsDTO createdSurvey = createDraft();

            authenticatedClient.put()
                    .uri("/api/v1/admin/surveys/{surveyId}", createdSurvey.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateCommand(createdSurvey, "Updated feedback"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Updated feedback")
                    .jsonPath("$.status").isEqualTo("DRAFT");

            publishSurvey(createdSurvey.id());

            authenticatedClient.put()
                    .uri("/api/v1/admin/surveys/{surveyId}", createdSurvey.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateCommand(createdSurvey, "Too late"))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Only draft surveys can be updated");
        }

        @Test
        @WithMockUser(role = "EMPLOYEE")
        @DisplayName("Should stop responses after a survey is closed manually")
        void closeSurvey_shouldRejectFurtherResponses() {

            useEmployeeAccount();
            AdminSurveyDetailsDTO createdSurvey = createDraft();
            publishSurvey(createdSurvey.id());

            authenticatedClient.post()
                    .uri("/api/v1/admin/surveys/{surveyId}/close", createdSurvey.id())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("CLOSED");

            useStudentAccount();
            authenticatedClient.post()
                    .uri("/api/v1/surveys/{surveyId}/responses", createdSurvey.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(submission(createdSurvey.questions().getFirst().id()))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Survey is not accepting responses");
        }
    }

    @Nested
    @DisplayName("Results visibility")
    class ResultsTests {

        @Test
        @WithMockUser(role = "STUDENT")
        @DisplayName("Should reveal aggregated results only after a visible survey is submitted")
        void getPublicResults_shouldDependOnSubmissionAndVisibility() {

            useEmployeeAccount();
            AdminSurveyDetailsDTO hiddenSurvey = createDraft(surveyCommand(
                    "Hidden results",
                    List.of(paceQuestion(null)),
                    true,
                    false,
                    false,
                    null
            ));
            publishSurvey(hiddenSurvey.id());
            useStudentAccount();
            submitAnswer(hiddenSurvey.id(), hiddenSurvey.questions().getFirst().id());

            authenticatedClient.get()
                    .uri("/api/v1/surveys/{surveyId}/public-results", hiddenSurvey.id())
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Survey results are hidden");

            useEmployeeAccount();
            AdminSurveyDetailsDTO visibleSurvey = createDraft(surveyCommand(
                    "Visible results",
                    List.of(paceQuestion(null)),
                    true,
                    false,
                    true,
                    null
            ));
            publishSurvey(visibleSurvey.id());
            useStudentAccount();

            authenticatedClient.get()
                    .uri("/api/v1/surveys/{surveyId}/public-results", visibleSurvey.id())
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Survey results are available after submission");

            submitAnswer(visibleSurvey.id(), visibleSurvey.questions().getFirst().id());

            authenticatedClient.get()
                    .uri("/api/v1/surveys/{surveyId}/public-results", visibleSurvey.id())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.responseCount").isEqualTo(1)
                    .jsonPath("$.questions[0].distribution['Too easy']").isEqualTo(1);

            useEmployeeAccount();
            authenticatedClient.get()
                    .uri("/api/v1/admin/surveys/{surveyId}/results", visibleSurvey.id())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.responseCount").isEqualTo(1)
                    .jsonPath("$.questions[0].distribution['Too easy']").isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Response limit and feeds")
    class CapacityAndFeedTests {

        @Test
        @WithMockUser(role = "STUDENT")
        @DisplayName("Should close the survey when the response limit is reached")
        void submitResponse_whenLimitReached_shouldCloseSurvey() {

            useEmployeeAccount();
            AdminSurveyDetailsDTO createdSurvey = createDraft(surveyCommand(
                    "Limited feedback",
                    List.of(paceQuestion(null)),
                    true,
                    false,
                    false,
                    1
            ));
            publishSurvey(createdSurvey.id());
            UserEntity secondStudent = createAndSaveUser(uuid(2), "student2@pjatk.edu.pl");
            useStudentAccount();
            submitAnswer(createdSurvey.id(), createdSurvey.questions().getFirst().id());

            useAccount(secondStudent.getEmail());
            authenticatedClient.post()
                    .uri("/api/v1/surveys/{surveyId}/responses", createdSurvey.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(submission(createdSurvey.questions().getFirst().id()))
                    .exchange()
                    .expectStatus().isEqualTo(409);

            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM surveys WHERE id = ?",
                    String.class,
                    createdSurvey.id()
            );
            assertThat(status).isEqualTo("CLOSED");
        }

        @Test
        @WithMockUser(role = "STUDENT")
        @DisplayName("Should move a submitted survey from pending to completed")
        void submitResponse_shouldMoveSurveyToCompletedFeed() {

            AdminSurveyDetailsDTO publishedSurvey = publishedAnonymousSurvey();
            useStudentAccount();
            String questionIdentifier = publishedSurvey.questions().getFirst().id();

            authenticatedClient.get()
                    .uri("/api/v1/surveys/pending")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content[?(@.id == '%s')]".formatted(publishedSurvey.id())).isNotEmpty();

            submitAnswer(publishedSurvey.id(), questionIdentifier);

            authenticatedClient.get()
                    .uri("/api/v1/surveys/pending")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content[?(@.id == '%s')]".formatted(publishedSurvey.id())).isEmpty();
            authenticatedClient.get()
                    .uri("/api/v1/surveys/completed")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.content[?(@.id == '%s')]".formatted(publishedSurvey.id())).isNotEmpty();
        }

        @Test
        @WithMockUser(role = "STUDENT")
        @DisplayName("Should keep the student identifier on a named response")
        void submitResponse_whenNamed_shouldStoreTheResponseOwner() {

            useEmployeeAccount();
            AdminSurveyDetailsDTO createdSurvey = createDraft(surveyCommand(
                    "Named feedback",
                    List.of(paceQuestion(null)),
                    false,
                    false,
                    false,
                    null
            ));
            publishSurvey(createdSurvey.id());
            useStudentAccount();
            submitAnswer(createdSurvey.id(), createdSurvey.questions().getFirst().id());

            UUID responseUserIdentifier = jdbcTemplate.queryForObject(
                    "SELECT user_id FROM survey_responses WHERE survey_id = ?",
                    UUID.class,
                    createdSurvey.id()
            );
            assertThat(responseUserIdentifier).isEqualTo(currentUser.getId());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @WithMockUser(role = "EMPLOYEE")
        @DisplayName("Should reject an anonymous survey that allows resubmission")
        void createSurvey_whenAnonymousAllowsResubmission_shouldReturnBadRequest() {

            useEmployeeAccount();

            authenticatedClient.post()
                    .uri("/api/v1/admin/surveys")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(surveyCommand(
                            "Invalid privacy",
                            List.of(paceQuestion(null)),
                            true,
                            true,
                            false,
                            null
                    ))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Anonymous surveys cannot allow resubmission");
        }

        @Test
        @WithMockUser(role = "STUDENT")
        @DisplayName("Should accept a skipped required question and reject it when the skip does not apply")
        void submitResponse_shouldHonorSkipLogic() {

            useEmployeeAccount();
            AdminSurveyDetailsDTO createdSurvey = createDraft(surveyCommand(
                    "Skip logic",
                    List.of(
                            new SurveyQuestionDTO("q1", QuestionType.SINGLE, "Choice", true, List.of("A", "B"), null, null, null, null,
                                    List.of(new SkipLogicDTO("A", "q3"))),
                            new SurveyQuestionDTO("q2", QuestionType.TEXT, "Hidden", true, List.of(), null, null, null, null, List.of()),
                            new SurveyQuestionDTO("q3", QuestionType.TEXT, "Last", true, List.of(), null, null, null, null, List.of())
                    ),
                    true,
                    false,
                    false,
                    null
            ));
            publishSurvey(createdSurvey.id());
            useStudentAccount();

            authenticatedClient.post()
                    .uri("/api/v1/surveys/{surveyId}/responses", createdSurvey.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new SubmitSurveyRequestDTO(List.of(
                            new AnswerDTO("q1", "A"),
                            new AnswerDTO("q3", "done")
                    )))
                    .exchange()
                    .expectStatus().isNoContent();

            useEmployeeAccount();
            AdminSurveyDetailsDTO secondSurvey = createDraft(surveyCommand(
                    "Skip logic missed",
                    List.of(
                            new SurveyQuestionDTO("q1", QuestionType.SINGLE, "Choice", true, List.of("A", "B"), null, null, null, null,
                                    List.of(new SkipLogicDTO("A", "q3"))),
                            new SurveyQuestionDTO("q2", QuestionType.TEXT, "Hidden", true, List.of(), null, null, null, null, List.of()),
                            new SurveyQuestionDTO("q3", QuestionType.TEXT, "Last", true, List.of(), null, null, null, null, List.of())
                    ),
                    true,
                    false,
                    false,
                    null
            ));
            publishSurvey(secondSurvey.id());
            useStudentAccount();

            authenticatedClient.post()
                    .uri("/api/v1/surveys/{surveyId}/responses", secondSurvey.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new SubmitSurveyRequestDTO(List.of(
                            new AnswerDTO("q1", "B"),
                            new AnswerDTO("q3", "done")
                    )))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("A required question is missing an answer");
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

        return createDraft(draftCommand());
    }

    private AdminSurveyDetailsDTO createDraft(CreateSurveyCommand command) {

        AdminSurveyDetailsDTO createdSurvey = authenticatedClient.post()
                .uri("/api/v1/admin/surveys")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
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

        return surveyCommand("Course feedback", List.of(paceQuestion(null)), true, false, false, null);
    }

    private CreateSurveyCommand surveyCommand(
            String title,
            List<SurveyQuestionDTO> questions,
            boolean anonymous,
            boolean allowResubmission,
            boolean showResultsToStudents,
            Integer responseLimit
    ) {

        return new CreateSurveyCommand(
                title,
                "How was the course?",
                questions,
                AudienceTargetType.ALL,
                List.of(),
                anonymous,
                allowResubmission,
                true,
                showResultsToStudents,
                false,
                responseLimit,
                null,
                null,
                null,
                List.of()
        );
    }

    private SurveyQuestionDTO paceQuestion(String id) {

        return new SurveyQuestionDTO(
                id,
                QuestionType.SINGLE,
                "Pace",
                true,
                List.of("Too easy", "Just right", "Too hard"),
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    private UpdateSurveyCommand updateCommand(AdminSurveyDetailsDTO survey, String title) {

        return new UpdateSurveyCommand(
                title,
                survey.description(),
                survey.questions(),
                survey.targetType(),
                survey.targetIds(),
                survey.anonymous(),
                survey.allowResubmission(),
                survey.requireLogin(),
                survey.showResultsToStudents(),
                survey.randomizeQuestionOrder(),
                survey.responseLimit(),
                survey.confirmationMessage(),
                survey.opensAt(),
                survey.closesAt(),
                survey.reminderIntervals()
        );
    }

    private void useEmployeeAccount() {

        if (userRepository.findByEmail("employee@pjatk.edu.pl").isEmpty()) {
            UserEntity employee = createAndSaveUser(uuid(1), "employee@pjatk.edu.pl");
            employee.setRole(Role.EMPLOYEE);
            userRepository.save(employee);
        }
        useAccount("employee@pjatk.edu.pl");
    }

    private void useStudentAccount() {

        useAccount("student@pjatk.edu.pl");
    }

    private void useAccount(String email) {

        when(jwtService.extractUsername(anyString())).thenReturn(email);
        when(jwtService.isTokenValid(anyString(), any())).thenReturn(true);
    }
}
