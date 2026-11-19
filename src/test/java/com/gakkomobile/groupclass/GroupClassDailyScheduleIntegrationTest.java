package com.gakkomobile.groupclass;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

class GroupClassDailyScheduleIntegrationTest extends GroupClassIntegrationTest {

    private static final String DAY_URI = "/api/v1/group-classes/day";
    private static final String STUDENT_EMAIL = "student@pjatk.edu.pl";
    private static final LocalDate DAY = LocalDate.of(2026, 12, 7);

    @BeforeEach
    void seedSchedule() {
        // the default student of the base class belongs to the first group
        UUID studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", UUID.class, STUDENT_EMAIL);
        addToGroup(studentId, groupId, false);

        // 10:00-11:30 recurring class for the first group (two slots, Monday and next Monday)
        UUID recurring = insertClass(subjectId, roomId, LocalTime.of(10, 0), LocalTime.of(11, 30),
                false, Set.of(groupId), Set.of(lecturerId));
        insertSlot(recurring, DAY, "teams-recurring-1");
        insertSlot(recurring, DAY.plusWeeks(1), "teams-recurring-2");

        // 08:15-09:45 one-time class for the first group, scheduled before the recurring one
        UUID oneTime = insertClass(secondSubjectId, secondRoomId, LocalTime.of(8, 15), LocalTime.of(9, 45),
                true, Set.of(groupId), Set.of(secondLecturerId));
        insertSlot(oneTime, DAY, null);

        // class of the second group on the same day, the default student must not see it
        UUID otherGroupClass = insertClass(subjectId, roomId, LocalTime.of(12, 0), LocalTime.of(13, 30),
                true, Set.of(secondGroupId), Set.of(lecturerId));
        insertSlot(otherGroupClass, DAY, "teams-other");
    }

    @Test
    @DisplayName("Should return the classes of a student for the day ordered by start time")
    void individual_shouldReturnStudentClassesOrderedByStartTime() {
        authenticatedClient.get()
                .uri(individual(STUDENT_EMAIL, DAY))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].subjectCode").isEqualTo("ENG")
                .jsonPath("$[0].classType").isEqualTo("language course")
                .jsonPath("$[0].building").isEqualTo("Main Campus")
                .jsonPath("$[0].room").isEqualTo("Lab 12")
                .jsonPath("$[0].classDate").isEqualTo("2026-12-07")
                .jsonPath("$[0].startTime").isEqualTo("08:15")
                .jsonPath("$[0].endTime").isEqualTo("09:45")
                .jsonPath("$[0].durationInMins").isEqualTo(90)
                .jsonPath("$[0].lecturers[0]").isEqualTo("Nowak Anna")
                .jsonPath("$[0].msTeamsCode").doesNotExist()
                .jsonPath("$[1].subjectCode").isEqualTo("PRG")
                .jsonPath("$[1].startTime").isEqualTo("10:00")
                .jsonPath("$[1].durationInMins").isEqualTo(90)
                .jsonPath("$[1].lecturers[0]").isEqualTo("Kowalski Jan")
                .jsonPath("$[1].groups[0]").isEqualTo("WIs I.1");
    }

    @Test
    @DisplayName("Should return an empty list when the student has no classes on that day")
    void individual_shouldReturnEmptyList_whenThereAreNoClassesOnTheDay() {
        authenticatedClient.get()
                .uri(individual(STUDENT_EMAIL, DAY.plusDays(1)))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("Should not return classes of groups the student does not belong to")
    void individual_shouldReturnEmptyList_forStudentWithoutGroup() {
        insertUser(Role.STUDENT, "loner@pjatk.edu.pl", "Lone", "Student");

        authenticatedClient.get()
                .uri(individual("loner@pjatk.edu.pl", DAY))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("Should return the classes taught by a lecturer, who is not a member of any group")
    void individual_shouldReturnLecturerClasses() {
        authenticatedClient.get()
                .uri(individual("jan.kowalski@pjatk.edu.pl", DAY))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].startTime").isEqualTo("10:00")
                .jsonPath("$[0].subjectCode").isEqualTo("PRG")
                .jsonPath("$[1].startTime").isEqualTo("12:00")
                .jsonPath("$[1].groups[0]").isEqualTo("WIs I.2");

        authenticatedClient.get()
                .uri(individual("anna.nowak@pjatk.edu.pl", DAY))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].subjectCode").isEqualTo("ENG");
    }

    @Test
    @DisplayName("Should return 404 when the user does not exist")
    void individual_shouldReturnNotFound_whenUserDoesNotExist() {
        authenticatedClient.get()
                .uri(individual("unknown@pjatk.edu.pl", DAY))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Should return 400 when the email is not provided for the individual filter")
    void individual_shouldReturnBadRequest_whenEmailIsMissing() {
        authenticatedClient.get()
                .uri(uri -> uri.path(DAY_URI)
                        .queryParam("filter-option", "INDIVIDUAL")
                        .queryParam("date", DAY)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.EMAIL_NOT_PROVIDED);
    }

    @Test
    @DisplayName("Should return the classes of a group for the day ordered by start time")
    void group_shouldReturnGroupClasses() {
        authenticatedClient.get()
                .uri(group(groupId, DAY))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].startTime").isEqualTo("08:15")
                .jsonPath("$[1].startTime").isEqualTo("10:00");

        authenticatedClient.get()
                .uri(group(secondGroupId, DAY))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].startTime").isEqualTo("12:00")
                .jsonPath("$[0].msTeamsCode").isEqualTo("teams-other");
    }

    @Test
    @DisplayName("Should return a class shared by two groups once, listing both groups")
    void group_shouldReturnSharedClassOnce() {
        UUID sharedClass = insertClass(subjectId, roomId, LocalTime.of(14, 0), LocalTime.of(15, 30),
                true, Set.of(groupId, secondGroupId), Set.of(lecturerId));
        insertSlot(sharedClass, DAY, null);

        authenticatedClient.get()
                .uri(group(groupId, DAY))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].startTime").isEqualTo("14:00")
                .jsonPath("$[2].groups.length()").isEqualTo(2);
    }

    @Test
    @DisplayName("Should return an empty list when the group has no classes on that day")
    void group_shouldReturnEmptyList_whenThereAreNoClassesOnTheDay() {
        authenticatedClient.get()
                .uri(group(groupId, DAY.minusDays(1)))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("Should return 404 when the group does not exist")
    void group_shouldReturnNotFound_whenGroupDoesNotExist() {
        authenticatedClient.get()
                .uri(group(UUID.randomUUID(), DAY))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.ACADEMIC_GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("Should return 400 when the group id is not provided for the group filter")
    void group_shouldReturnBadRequest_whenGroupIdIsMissing() {
        authenticatedClient.get()
                .uri(uri -> uri.path(DAY_URI)
                        .queryParam("filter-option", "GROUP")
                        .queryParam("date", DAY)
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should return 400 when the date is missing, malformed or the filter option is unknown")
    void shouldReturnBadRequest_forInvalidParameters() {
        authenticatedClient.get()
                .uri(uri -> uri.path(DAY_URI)
                        .queryParam("filter-option", "INDIVIDUAL")
                        .queryParam("user-email", STUDENT_EMAIL)
                        .build())
                .exchange()
                .expectStatus().isBadRequest();

        authenticatedClient.get()
                .uri(uri -> uri.path(DAY_URI)
                        .queryParam("filter-option", "INDIVIDUAL")
                        .queryParam("user-email", STUDENT_EMAIL)
                        .queryParam("date", "07-12-2026")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();

        authenticatedClient.get()
                .uri(uri -> uri.path(DAY_URI)
                        .queryParam("filter-option", "EVERYONE")
                        .queryParam("date", DAY)
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should not return a deleted slot")
    void individual_shouldNotReturnDeletedSlot() {
        jdbcTemplate.update("DELETE FROM class_slots WHERE ms_teams_code = 'teams-recurring-1'");

        authenticatedClient.get()
                .uri(individual(STUDENT_EMAIL, DAY))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].subjectCode").isEqualTo("ENG");
    }

    @Test
    @DisplayName("Should reject an unauthenticated request")
    void shouldReturnForbidden_whenUnauthenticated() {
        anonymousClient.get()
                .uri(individual(STUDENT_EMAIL, DAY))
                .exchange()
                .expectStatus().isForbidden();
    }

    private Function<UriBuilder, java.net.URI> individual(String email, LocalDate date) {
        return uri -> uri.path(DAY_URI)
                .queryParam("filter-option", "INDIVIDUAL")
                .queryParam("user-email", email)
                .queryParam("date", date)
                .build();
    }

    private Function<UriBuilder, java.net.URI> group(UUID id, LocalDate date) {
        return uri -> uri.path(DAY_URI)
                .queryParam("filter-option", "GROUP")
                .queryParam("group-id", id)
                .queryParam("date", date)
                .build();
    }
}
