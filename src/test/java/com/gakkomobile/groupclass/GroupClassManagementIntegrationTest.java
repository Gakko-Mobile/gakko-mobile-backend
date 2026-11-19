package com.gakkomobile.groupclass;

import static org.assertj.core.api.Assertions.assertThat;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class GroupClassManagementIntegrationTest extends GroupClassIntegrationTest {

    private static final String CLASSES_URI = "/api/v1/group-classes";

    // ---------------------------------------------------------------- POST /

    @Nested
    class CreateGroupClass {

        @Test
        @DisplayName("Should create a one-time class with a single slot")
        void shouldCreateOneTimeClass() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI)
                    .bodyValue(createRequest(true, List.of(slot("2026-12-14", "teams-abc"))))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.subjectCode").isEqualTo("PRG")
                    .jsonPath("$.building").isEqualTo("Main Campus")
                    .jsonPath("$.room").isEqualTo("Aula A")
                    .jsonPath("$.startTime").isEqualTo("08:15")
                    .jsonPath("$.endTime").isEqualTo("09:45")
                    .jsonPath("$.classDate").isEqualTo("2026-12-14")
                    .jsonPath("$.groups[0]").isEqualTo("WIs I.1")
                    .jsonPath("$.lecturers[0]").isEqualTo("Kowalski Jan")
                    .jsonPath("$.slots.length()").isEqualTo(1)
                    .jsonPath("$.slots[0].date").isEqualTo("2026-12-14")
                    .jsonPath("$.slots[0].msTeamsCode").isEqualTo("teams-abc");

            assertThat(count("group_classes")).isEqualTo(1);
            assertThat(count("class_slots")).isEqualTo(1);
            assertThat(count("academic_groups_group_classes")).isEqualTo(1);
            assertThat(count("group_class_lecturers")).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT is_one_time FROM group_classes", Boolean.class)).isTrue();
        }

        @Test
        @DisplayName("Should create a recurring class with many slots returned in date order")
        void shouldCreateRecurringClass() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI)
                    .bodyValue(createRequest(false, List.of(
                            slot("2026-12-21", null),
                            slot("2026-12-07", "teams-1"),
                            slot("2026-12-14", null)
                    )))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.classDate").isEqualTo("2026-12-07")
                    .jsonPath("$.slots.length()").isEqualTo(3)
                    .jsonPath("$.slots[0].date").isEqualTo("2026-12-07")
                    .jsonPath("$.slots[0].msTeamsCode").isEqualTo("teams-1")
                    .jsonPath("$.slots[1].date").isEqualTo("2026-12-14")
                    .jsonPath("$.slots[2].date").isEqualTo("2026-12-21");

            assertThat(count("class_slots")).isEqualTo(3);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT is_one_time FROM group_classes", Boolean.class)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = Role.class, names = {"STUDENT", "LECTURER", "USER"})
        @DisplayName("Should return 403 for roles other than employee and admin")
        void shouldReturnForbidden_forOtherRoles(Role role) {
            // given
            loginAs(role);

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI)
                    .bodyValue(createRequest(true, List.of(slot("2026-12-14", null))))
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(count("group_classes")).isZero();
        }

        @Test
        @DisplayName("Should return 400 when a one-time class has more than one slot")
        void shouldReturnBadRequest_whenOneTimeClassHasManySlots() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI)
                    .bodyValue(createRequest(true, List.of(slot("2026-12-14", null), slot("2026-12-21", null))))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.ONE_TIME_CLASS_MULTIPLE_SLOTS);

            assertThat(count("group_classes")).isZero();
        }

        @Test
        @DisplayName("Should return 400 when slot dates are duplicated")
        void shouldReturnBadRequest_whenSlotDatesAreDuplicated() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI)
                    .bodyValue(createRequest(false, List.of(slot("2026-12-14", null), slot("2026-12-14", "x"))))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.DUPLICATE_SLOT_DATES);

            assertThat(count("group_classes")).isZero();
        }

        @Test
        @DisplayName("Should return 400 when the end time is not after the start time")
        void shouldReturnBadRequest_whenEndTimeIsNotAfterStartTime() {
            // given
            loginAs(Role.EMPLOYEE);

            Map<String, Object> request = createRequest(true, List.of(slot("2026-12-14", null)));
            request.put("startTime", "10:00");
            request.put("endTime", "09:00");

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message")
                    .isEqualTo(ExceptionErrorConstants.SCHEDULE_SLOT_END_TIME_NOT_AFTER_START_TIME);
        }

        @Test
        @DisplayName("Should return 400 with validation errors when required fields are missing")
        void shouldReturnBadRequest_whenFieldsAreMissing() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI)
                    .bodyValue(Map.of())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.validationErrors.subjectId").exists()
                    .jsonPath("$.validationErrors.roomId").exists()
                    .jsonPath("$.validationErrors.groupIds").exists()
                    .jsonPath("$.validationErrors.lecturerIds").exists()
                    .jsonPath("$.validationErrors.startTime").exists()
                    .jsonPath("$.validationErrors.endTime").exists()
                    .jsonPath("$.validationErrors.oneTime").exists()
                    .jsonPath("$.validationErrors.slots").exists();
        }

        @Test
        @DisplayName("Should return 400 when the teams code is longer than 100 characters")
        void shouldReturnBadRequest_whenTeamsCodeIsTooLong() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI)
                    .bodyValue(createRequest(true, List.of(slot("2026-12-14", "x".repeat(101)))))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.validationErrors").exists();
        }

        @Test
        @DisplayName("Should return 400 when a lecturer id belongs to a non-lecturer")
        void shouldReturnBadRequest_whenUserIsNotALecturer() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID employeeId = insertUser(Role.EMPLOYEE, "other.employee@pjatk.edu.pl", "Other", "Employee").getId();

            Map<String, Object> request = createRequest(true, List.of(slot("2026-12-14", null)));
            request.put("lecturerIds", List.of(employeeId.toString()));

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message")
                    .isEqualTo(ExceptionErrorConstants.USER_IS_NOT_A_LECTURER.formatted(employeeId));

            assertThat(count("group_classes")).isZero();
        }

        @Test
        @DisplayName("Should return 404 when the subject, room, group or lecturer does not exist")
        void shouldReturnNotFound_whenReferencedEntityDoesNotExist() {
            // given
            loginAs(Role.EMPLOYEE);
            String unknown = UUID.randomUUID().toString();

            // when and then
            for (Map.Entry<String, Object> replacement : Map.<String, Object>of(
                    "subjectId", unknown,
                    "roomId", unknown,
                    "groupIds", List.of(unknown),
                    "lecturerIds", List.of(unknown)
            ).entrySet()) {
                Map<String, Object> request = createRequest(true, List.of(slot("2026-12-14", null)));
                request.put(replacement.getKey(), replacement.getValue());

                authenticatedClient.post()
                        .uri(CLASSES_URI)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isNotFound();
            }

            assertThat(count("group_classes")).isZero();
        }
    }

    // ---------------------------------------------------------------- PUT /{id}

    @Nested
    class UpdateGroupClass {

        @Test
        @DisplayName("Should replace the class details and keep its slots")
        void shouldUpdateDetailsAndKeepSlots() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07", "2026-12-14");

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId)
                    .bodyValue(updateRequest())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.subjectCode").isEqualTo("ENG")
                    .jsonPath("$.classType").isEqualTo("language course")
                    .jsonPath("$.room").isEqualTo("Lab 12")
                    .jsonPath("$.startTime").isEqualTo("10:00")
                    .jsonPath("$.endTime").isEqualTo("11:30")
                    .jsonPath("$.groups.length()").isEqualTo(2)
                    .jsonPath("$.lecturers.length()").isEqualTo(1)
                    .jsonPath("$.lecturers[0]").isEqualTo("Nowak Anna")
                    .jsonPath("$.slots.length()").isEqualTo(2);

            assertThat(count("group_classes")).isEqualTo(1);
            assertThat(count("class_slots")).isEqualTo(2);
            assertThat(count("academic_groups_group_classes")).isEqualTo(2);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT lecturer_id FROM group_class_lecturers", UUID.class)).isEqualTo(secondLecturerId);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT start_time FROM group_classes", LocalTime.class)).isEqualTo(LocalTime.of(10, 0));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT is_one_time FROM group_classes", Boolean.class)).isFalse();
        }

        @Test
        @DisplayName("Should return 404 when the class does not exist")
        void shouldReturnNotFound_whenClassDoesNotExist() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + UUID.randomUUID())
                    .bodyValue(updateRequest())
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.GROUP_CLASS_NOT_FOUND);
        }

        @Test
        @DisplayName("Should return 400 and keep the class unchanged when the time range is invalid")
        void shouldReturnBadRequest_whenTimeRangeIsInvalid() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07");

            Map<String, Object> request = updateRequest();
            request.put("startTime", "12:00");
            request.put("endTime", "11:00");

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT start_time FROM group_classes", LocalTime.class)).isEqualTo(LocalTime.of(8, 15));
        }

        @Test
        @DisplayName("Should return 400 when a lecturer id belongs to a non-lecturer")
        void shouldReturnBadRequest_whenUserIsNotALecturer() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07");

            Map<String, Object> request = updateRequest();
            request.put("lecturerIds", List.of(currentUserIdOf("employee@pjatk.edu.pl").toString()));

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT lecturer_id FROM group_class_lecturers", UUID.class)).isEqualTo(lecturerId);
        }

        @Test
        @DisplayName("Should return 400 with validation errors when required fields are missing")
        void shouldReturnBadRequest_whenFieldsAreMissing() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId)
                    .bodyValue(Map.of())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.validationErrors.subjectId").exists()
                    .jsonPath("$.validationErrors.lecturerIds").exists();
        }

        @Test
        @DisplayName("Should return 403 for a student")
        void shouldReturnForbidden_forStudent() {
            // given
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId)
                    .bodyValue(updateRequest())
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    // ---------------------------------------------------------------- DELETE /{id}

    @Nested
    class DeleteGroupClass {

        @Test
        @DisplayName("Should delete the class together with its slots and links")
        void shouldDeleteClassWithSlotsAndLinks() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07", "2026-12-14");
            UUID otherClassId = recurringClass("2026-12-08");

            // when and then
            authenticatedClient.delete()
                    .uri(CLASSES_URI + "/" + classId)
                    .exchange()
                    .expectStatus().isNoContent();

            assertThat(count("group_classes")).isEqualTo(1);
            assertThat(count("class_slots")).isEqualTo(1);
            assertThat(count("academic_groups_group_classes")).isEqualTo(1);
            assertThat(count("group_class_lecturers")).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT id FROM group_classes", UUID.class))
                    .isEqualTo(otherClassId);
        }

        @Test
        @DisplayName("Should return 404 when the class does not exist")
        void shouldReturnNotFound_whenClassDoesNotExist() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.delete()
                    .uri(CLASSES_URI + "/" + UUID.randomUUID())
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("Should return 403 for a student and keep the class")
        void shouldReturnForbidden_forStudent() {
            // given
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.delete()
                    .uri(CLASSES_URI + "/" + classId)
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(count("group_classes")).isEqualTo(1);
        }
    }

    // ---------------------------------------------------------------- POST /{id}/slots

    @Nested
    class CreateClassSlot {

        @Test
        @DisplayName("Should add a slot to a recurring class and return all slots")
        void shouldAddSlotToRecurringClass() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07", "2026-12-21");

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI + "/" + classId + "/slots")
                    .bodyValue(slot("2026-12-14", "teams-new"))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.slots.length()").isEqualTo(3)
                    .jsonPath("$.slots[0].date").isEqualTo("2026-12-07")
                    .jsonPath("$.slots[1].date").isEqualTo("2026-12-14")
                    .jsonPath("$.slots[1].msTeamsCode").isEqualTo("teams-new")
                    .jsonPath("$.slots[2].date").isEqualTo("2026-12-21");

            assertThat(count("class_slots")).isEqualTo(3);
        }

        @Test
        @DisplayName("Should add the first slot to a one-time class that has none")
        void shouldAddSlotToEmptyOneTimeClass() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = insertClass(subjectId, roomId, LocalTime.of(8, 15), LocalTime.of(9, 45),
                    true, Set.of(groupId), Set.of(lecturerId));

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI + "/" + classId + "/slots")
                    .bodyValue(slot("2026-12-14", null))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.slots.length()").isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 409 when a one-time class already has a slot")
        void shouldReturnConflict_whenOneTimeClassAlreadyHasSlot() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = oneTimeClass("2026-12-07");

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI + "/" + classId + "/slots")
                    .bodyValue(slot("2026-12-14", null))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.ONE_TIME_CLASS_ALREADY_HAS_SLOT);

            assertThat(count("class_slots")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 409 when the class already has a slot on that date")
        void shouldReturnConflict_whenDateIsTaken() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI + "/" + classId + "/slots")
                    .bodyValue(slot("2026-12-07", null))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.CLASS_SLOT_ALREADY_EXISTS);

            assertThat(count("class_slots")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 404 when the class does not exist")
        void shouldReturnNotFound_whenClassDoesNotExist() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI + "/" + UUID.randomUUID() + "/slots")
                    .bodyValue(slot("2026-12-14", null))
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("Should return 400 when the class date is missing")
        void shouldReturnBadRequest_whenDateIsMissing() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI + "/" + classId + "/slots")
                    .bodyValue(Map.of())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.validationErrors.classDate").exists();
        }

        @Test
        @DisplayName("Should return 403 for a student")
        void shouldReturnForbidden_forStudent() {
            // given
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.post()
                    .uri(CLASSES_URI + "/" + classId + "/slots")
                    .bodyValue(slot("2026-12-14", null))
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    // ---------------------------------------------------------------- PUT /{id}/slots/{date}

    @Nested
    class UpdateClassSlot {

        @Test
        @DisplayName("Should move the slot to a new date and set the teams code")
        void shouldMoveSlotAndSetTeamsCode() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07", "2026-12-14");

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId + "/slots/2026-12-07")
                    .bodyValue(slot("2026-12-09", "teams-2"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.slots.length()").isEqualTo(2)
                    .jsonPath("$.slots[0].date").isEqualTo("2026-12-09")
                    .jsonPath("$.slots[0].msTeamsCode").isEqualTo("teams-2")
                    .jsonPath("$.slots[1].date").isEqualTo("2026-12-14");

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class_slots WHERE class_date = DATE '2026-12-07'", Long.class)).isZero();
        }

        @Test
        @DisplayName("Should change only the teams code when the date stays the same")
        void shouldChangeOnlyTeamsCode() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId + "/slots/2026-12-07")
                    .bodyValue(slot("2026-12-07", "teams-only"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.slots[0].date").isEqualTo("2026-12-07")
                    .jsonPath("$.slots[0].msTeamsCode").isEqualTo("teams-only");

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT ms_teams_code FROM class_slots", String.class)).isEqualTo("teams-only");
        }

        @Test
        @DisplayName("Should return 409 when moving the slot onto a date that already has one")
        void shouldReturnConflict_whenTargetDateIsTaken() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07", "2026-12-14");

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId + "/slots/2026-12-07")
                    .bodyValue(slot("2026-12-14", null))
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.CLASS_SLOT_ALREADY_EXISTS);

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM class_slots WHERE class_date = DATE '2026-12-07'", Long.class))
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("Should return 404 when the slot does not exist")
        void shouldReturnNotFound_whenSlotDoesNotExist() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId + "/slots/2027-01-01")
                    .bodyValue(slot("2027-01-02", null))
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.CLASS_SLOT_NOT_FOUND);
        }

        @Test
        @DisplayName("Should return 404 when the class does not exist")
        void shouldReturnNotFound_whenClassDoesNotExist() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + UUID.randomUUID() + "/slots/2026-12-07")
                    .bodyValue(slot("2026-12-08", null))
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.GROUP_CLASS_NOT_FOUND);
        }

        @Test
        @DisplayName("Should return 400 when the date in the path is malformed")
        void shouldReturnBadRequest_whenPathDateIsMalformed() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId + "/slots/not-a-date")
                    .bodyValue(slot("2026-12-08", null))
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Should return 403 for a student")
        void shouldReturnForbidden_forStudent() {
            // given
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.put()
                    .uri(CLASSES_URI + "/" + classId + "/slots/2026-12-07")
                    .bodyValue(slot("2026-12-08", null))
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    // ---------------------------------------------------------------- DELETE /{id}/slots/{date}

    @Nested
    class DeleteClassSlot {

        @Test
        @DisplayName("Should delete only the slot on the given date and return the remaining ones")
        void shouldDeleteSingleSlot() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07", "2026-12-14", "2026-12-21");

            // when and then
            authenticatedClient.delete()
                    .uri(CLASSES_URI + "/" + classId + "/slots/2026-12-14")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.slots.length()").isEqualTo(2)
                    .jsonPath("$.slots[0].date").isEqualTo("2026-12-07")
                    .jsonPath("$.slots[1].date").isEqualTo("2026-12-21");

            assertThat(count("class_slots")).isEqualTo(2);
            assertThat(count("group_classes")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should keep the class when its last slot is deleted")
        void shouldKeepClassWhenLastSlotIsDeleted() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.delete()
                    .uri(CLASSES_URI + "/" + classId + "/slots/2026-12-07")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.slots.length()").isEqualTo(0)
                    .jsonPath("$.classDate").doesNotExist();

            assertThat(count("class_slots")).isZero();
            assertThat(count("group_classes")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 404 when the slot does not exist")
        void shouldReturnNotFound_whenSlotDoesNotExist() {
            // given
            loginAs(Role.EMPLOYEE);
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.delete()
                    .uri(CLASSES_URI + "/" + classId + "/slots/2027-01-01")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo(ExceptionErrorConstants.CLASS_SLOT_NOT_FOUND);

            assertThat(count("class_slots")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 404 when the class does not exist")
        void shouldReturnNotFound_whenClassDoesNotExist() {
            // given
            loginAs(Role.EMPLOYEE);

            // when and then
            authenticatedClient.delete()
                    .uri(CLASSES_URI + "/" + UUID.randomUUID() + "/slots/2026-12-07")
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("Should return 403 for a student")
        void shouldReturnForbidden_forStudent() {
            // given
            UUID classId = recurringClass("2026-12-07");

            // when and then
            authenticatedClient.delete()
                    .uri(CLASSES_URI + "/" + classId + "/slots/2026-12-07")
                    .exchange()
                    .expectStatus().isForbidden();

            assertThat(count("class_slots")).isEqualTo(1);
        }
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, Object> createRequest(boolean oneTime, List<Map<String, Object>> slots) {
        Map<String, Object> request = new HashMap<>();
        request.put("subjectId", subjectId.toString());
        request.put("roomId", roomId.toString());
        request.put("groupIds", List.of(groupId.toString()));
        request.put("lecturerIds", List.of(lecturerId.toString()));
        request.put("startTime", "08:15");
        request.put("endTime", "09:45");
        request.put("oneTime", oneTime);
        request.put("slots", slots);
        return request;
    }

    private Map<String, Object> updateRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("subjectId", secondSubjectId.toString());
        request.put("roomId", secondRoomId.toString());
        request.put("groupIds", List.of(groupId.toString(), secondGroupId.toString()));
        request.put("lecturerIds", List.of(secondLecturerId.toString()));
        request.put("startTime", "10:00");
        request.put("endTime", "11:30");
        return request;
    }

    private Map<String, Object> slot(String date, String msTeamsCode) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("classDate", date);
        slot.put("msTeamsCode", msTeamsCode);
        return slot;
    }

    private UUID recurringClass(String... dates) {
        return classWithSlots(false, dates);
    }

    private UUID oneTimeClass(String date) {
        return classWithSlots(true, date);
    }

    private UUID classWithSlots(boolean oneTime, String... dates) {
        UUID classId = insertClass(subjectId, roomId, LocalTime.of(8, 15), LocalTime.of(9, 45),
                oneTime, Set.of(groupId), Set.of(lecturerId));
        for (String date : dates) {
            insertSlot(classId, LocalDate.parse(date), null);
        }
        return classId;
    }

    private UUID currentUserIdOf(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);
    }
}
