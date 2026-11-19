package com.gakkomobile.groupclass;

import static org.assertj.core.api.Assertions.assertThat;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class GroupClassScheduleIntegrationTest extends GroupClassIntegrationTest {

    private static final String SCHEDULE_URI = "/api/v1/group-classes/schedule";

    @Test
    @DisplayName("Should create one recurring class per weekly entry with a slot for every matching date")
    void createSchedule_shouldPersistRecurringClassesWithSlots() {
        loginAs(Role.EMPLOYEE);

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(validRequest())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].classDate").isEqualTo("2026-10-05")
                .jsonPath("$[0].startTime").isEqualTo("08:30")
                .jsonPath("$[0].endTime").isEqualTo("10:00")
                .jsonPath("$[0].subjectName").isEqualTo("Java Programming")
                .jsonPath("$[0].subjectCode").isEqualTo("PRG")
                .jsonPath("$[0].classType").isEqualTo("lecture")
                .jsonPath("$[0].building").isEqualTo("Main Campus")
                .jsonPath("$[0].room").isEqualTo("Aula A")
                .jsonPath("$[0].groups[0]").isEqualTo("WIs I.1")
                .jsonPath("$[0].lecturers[0]").isEqualTo("Kowalski Jan")
                .jsonPath("$[0].slots.length()").isEqualTo(3)
                .jsonPath("$[0].slots[0].date").isEqualTo("2026-10-05")
                .jsonPath("$[0].slots[1].date").isEqualTo("2026-10-12")
                .jsonPath("$[0].slots[2].date").isEqualTo("2026-10-19")
                .jsonPath("$[1].classDate").isEqualTo("2026-10-07")
                .jsonPath("$[1].startTime").isEqualTo("12:00")
                .jsonPath("$[1].slots.length()").isEqualTo(3)
                .jsonPath("$[1].slots[2].date").isEqualTo("2026-10-21");

        assertThat(count("group_classes")).isEqualTo(2);
        assertThat(count("class_slots")).isEqualTo(6);
        assertThat(count("academic_groups_group_classes")).isEqualTo(2);
        assertThat(count("group_class_lecturers")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM group_classes WHERE is_one_time = FALSE", Long.class)).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should attach every requested group and lecturer to the created class")
    void createSchedule_shouldLinkAllGroupsAndLecturers() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        entry(request, 0).put("groupIds", List.of(groupId.toString(), secondGroupId.toString()));
        entry(request, 0).put("lecturerIds", List.of(lecturerId.toString(), secondLecturerId.toString()));

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$[0].groups.length()").isEqualTo(2)
                .jsonPath("$[0].lecturers.length()").isEqualTo(2);

        assertThat(count("academic_groups_group_classes")).isEqualTo(3);
        assertThat(count("group_class_lecturers")).isEqualTo(3);
    }

    @Test
    @DisplayName("Should count regular and ITN students of the groups in the response")
    void createSchedule_shouldReturnStudentCounts() {
        // given
        loginAs(Role.EMPLOYEE);
        addToGroup(insertUser(Role.STUDENT, "s1@pjatk.edu.pl", "A", "A").getId(), groupId, false);
        addToGroup(insertUser(Role.STUDENT, "s2@pjatk.edu.pl", "B", "B").getId(), groupId, false);
        addToGroup(insertUser(Role.STUDENT, "s3@pjatk.edu.pl", "C", "C").getId(), groupId, true);

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(validRequest())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$[0].regularStudentsNum").isEqualTo(2)
                .jsonPath("$[0].itnStudentsNum").isEqualTo(1);
    }

    @Test
    @DisplayName("Should allow an administrator to create a schedule")
    void createSchedule_shouldAllowAdmin() {
        // given
        loginAs(Role.ADMIN);

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(validRequest())
                .exchange()
                .expectStatus().isCreated();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"STUDENT", "LECTURER", "USER"})
    @DisplayName("Should reject the request with 403 for roles other than employee and admin")
    void createSchedule_shouldReturnForbidden_forOtherRoles(Role role) {
        // given
        loginAs(role);

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(validRequest())
                .exchange()
                .expectStatus().isForbidden();

        assertThat(count("group_classes")).isZero();
    }

    @Test
    @DisplayName("Should reject an unauthenticated request")
    void createSchedule_shouldReturnForbidden_whenUnauthenticated() {
        // when and then
        anonymousClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(validRequest())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should reject the request with 400 when there are no group classes")
    void createSchedule_shouldReturnBadRequest_whenGroupClassesAreEmpty() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        request.put("groupClasses", List.of());

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.validationErrors.groupClasses").exists();
    }

    @Test
    @DisplayName("Should reject the request with 400 when required fields are missing")
    void createSchedule_shouldReturnBadRequest_whenFieldsAreMissing() {
        // given
        loginAs(Role.EMPLOYEE);

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.validationErrors.startDate").exists()
                .jsonPath("$.validationErrors.endDate").exists()
                .jsonPath("$.validationErrors.groupClasses").exists();
    }

    @Test
    @DisplayName("Should reject the request with 400 when the end date is before the start date")
    void createSchedule_shouldReturnBadRequest_whenEndDateIsBeforeStartDate() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        request.put("startDate", "2026-10-21");
        request.put("endDate", "2026-10-05");

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.SCHEDULE_END_DATE_BEFORE_START_DATE);

        assertThat(count("group_classes")).isZero();
    }

    @Test
    @DisplayName("Should reject the request with 400 when the range is longer than five months")
    void createSchedule_shouldReturnBadRequest_whenRangeIsTooLong() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        request.put("startDate", "2026-10-05");
        request.put("endDate", "2027-03-06");

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo(ExceptionErrorConstants.SCHEDULE_DATE_RANGE_TOO_LONG.formatted(5));

        assertThat(count("group_classes")).isZero();
    }

    @Test
    @DisplayName("Should accept a range of exactly five months")
    void createSchedule_shouldAcceptRangeOfExactlyFiveMonths() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        request.put("startDate", "2026-10-05");
        request.put("endDate", "2027-03-05");

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    @DisplayName("Should reject the request with 400 when a class ends before it starts")
    void createSchedule_shouldReturnBadRequest_whenEndTimeIsNotAfterStartTime() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        entry(request, 0).put("startTime", "10:00");
        entry(request, 0).put("endTime", "10:00");

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo(ExceptionErrorConstants.SCHEDULE_SLOT_END_TIME_NOT_AFTER_START_TIME);

        assertThat(count("group_classes")).isZero();
    }

    @Test
    @DisplayName("Should reject the request with 400 when a lecturer id belongs to a non-lecturer")
    void createSchedule_shouldReturnBadRequest_whenUserIsNotALecturer() {
        // given
        loginAs(Role.EMPLOYEE);
        UUID studentId = insertUser(Role.STUDENT, "student.one@pjatk.edu.pl", "Stu", "Dent").getId();

        Map<String, Object> request = validRequest();
        entry(request, 0).put("lecturerIds", List.of(studentId.toString()));

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo(ExceptionErrorConstants.USER_IS_NOT_A_LECTURER.formatted(studentId));

        assertThat(count("group_classes")).isZero();
    }

    @Test
    @DisplayName("Should reject the request with 404 when the subject does not exist")
    void createSchedule_shouldReturnNotFound_whenSubjectDoesNotExist() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        entry(request, 0).put("subjectId", UUID.randomUUID().toString());

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.SUBJECT_NOT_FOUND);

        assertThat(count("group_classes")).isZero();
    }

    @Test
    @DisplayName("Should reject the request with 404 when the room does not exist")
    void createSchedule_shouldReturnNotFound_whenRoomDoesNotExist() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        entry(request, 0).put("roomId", UUID.randomUUID().toString());

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("Should reject the request with 404 when a group does not exist")
    void createSchedule_shouldReturnNotFound_whenGroupDoesNotExist() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        entry(request, 0).put("groupIds", List.of(groupId.toString(), UUID.randomUUID().toString()));

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        assertThat(count("group_classes")).isZero();
    }

    @Test
    @DisplayName("Should reject the request with 404 when a lecturer does not exist")
    void createSchedule_shouldReturnNotFound_whenLecturerDoesNotExist() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        entry(request, 0).put("lecturerIds", List.of(UUID.randomUUID().toString()));

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.LECTURER_NOT_FOUND);
    }

    @Test
    @DisplayName("Should not persist anything when one of the entries is invalid")
    void createSchedule_shouldPersistNothing_whenSecondEntryFails() {
        // given
        loginAs(Role.EMPLOYEE);

        Map<String, Object> request = validRequest();
        entry(request, 1).put("roomId", UUID.randomUUID().toString());

        // when and then
        authenticatedClient.post()
                .uri(SCHEDULE_URI)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        assertThat(count("group_classes")).isZero();
        assertThat(count("class_slots")).isZero();
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, Object> validRequest() {
        Map<String, Object> monday = new HashMap<>();
        monday.put("subjectId", subjectId.toString());
        monday.put("roomId", roomId.toString());
        monday.put("groupIds", List.of(groupId.toString()));
        monday.put("lecturerIds", List.of(lecturerId.toString()));
        monday.put("weekDay", "MONDAY");
        monday.put("startTime", "08:30");
        monday.put("endTime", "10:00");

        Map<String, Object> wednesday = new HashMap<>(monday);
        wednesday.put("weekDay", "WEDNESDAY");
        wednesday.put("startTime", "12:00");
        wednesday.put("endTime", "13:30");

        Map<String, Object> request = new HashMap<>();
        request.put("startDate", "2026-10-05");
        request.put("endDate", "2026-10-21");
        request.put("groupClasses", List.of(monday, wednesday));
        return request;
    }

    /**
     * Returns the (mutable) schedule entry at the given position of the request built by {@link #validRequest()}.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> entry(Map<String, Object> request, int index) {
        return (Map<String, Object>) ((List<?>) request.get("groupClasses")).get(index);
    }
}
