package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupEntity;
import com.gakkomobile.building.BuildingEntity;
import com.gakkomobile.groupclass.dto.ClassSlotDTO;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.dto.GroupClassWithSlotsResponseDTO;
import com.gakkomobile.groupmembership.GroupMembershipEntity;
import com.gakkomobile.room.RoomEntity;
import com.gakkomobile.subject.SubjectEntity;
import com.gakkomobile.subject.SubjectType;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GroupClassMapperTest {

    private final GroupClassMapper mapper = Mappers.getMapper(GroupClassMapper.class);

    private GroupClassEntity groupClass;

    @BeforeEach
    void setUp() {
        BuildingEntity building = new BuildingEntity();
        building.setName("Main Campus");

        RoomEntity room = new RoomEntity();
        room.setName("Aula A");
        room.setBuilding(building);

        SubjectEntity subject = new SubjectEntity();
        subject.setName("English B22");
        subject.setCode("ENG");
        subject.setType(SubjectType.LANGUAGE_COURSE);

        AcademicGroupEntity group1 = groupWithStudents("WIs I.1", 2, 1);
        AcademicGroupEntity group2 = groupWithStudents("WIs I.2", 0, 1);

        groupClass = new GroupClassEntity();
        groupClass.setSubject(subject);
        groupClass.setRoom(room);
        groupClass.setGroups(new HashSet<>(Set.of(group1, group2)));
        groupClass.setLecturers(new HashSet<>(Set.of(
                lecturer("Jan", "Kowalski"), lecturer("Anna", "Nowak"))));
        groupClass.setStartTime(LocalTime.of(8, 15));
        groupClass.setEndTime(LocalTime.of(9, 45));
    }

    // ---------------------------------------------------------------- toDto(slot)

    @Test
    void toDto_shouldMapAllFieldsFromSlotAndItsClass() {
        // given
        ClassSlotEntity slot = slot(LocalDate.of(2026, 10, 3), "teams-123");

        // when
        GroupClassResponseDTO dto = mapper.toDto(slot);

        // then
        assertThat(dto.subjectName()).isEqualTo("English B22");
        assertThat(dto.subjectCode()).isEqualTo("ENG");
        assertThat(dto.classType()).isEqualTo("language course");
        assertThat(dto.building()).isEqualTo("Main Campus");
        assertThat(dto.room()).isEqualTo("Aula A");
        assertThat(dto.classDate()).isEqualTo("2026-10-03");
        assertThat(dto.startTime()).isEqualTo("08:15");
        assertThat(dto.endTime()).isEqualTo("09:45");
        assertThat(dto.durationInMins()).isEqualTo(90);
        assertThat(dto.msTeamsCode()).isEqualTo("teams-123");
        assertThat(dto.groups()).containsExactlyInAnyOrder("WIs I.1", "WIs I.2");
        assertThat(dto.lecturers()).containsExactlyInAnyOrder("Kowalski Jan", "Nowak Anna");
        assertThat(dto.regularStudentsNum()).isEqualTo(2);
        assertThat(dto.itnStudentsNum()).isEqualTo(2);
    }

    @Test
    void toDto_shouldReturnNullTeamsCode_whenSlotHasNone() {
        // when
        GroupClassResponseDTO dto = mapper.toDto(slot(LocalDate.of(2026, 10, 3), null));

        // then
        assertThat(dto.msTeamsCode()).isNull();
    }

    // ---------------------------------------------------------------- toDtoWithSlots

    @Test
    void toDtoWithSlots_shouldMapClassFieldsAndExposeEarliestSlotDate() {
        // given
        groupClass.getSlots().add(slot(LocalDate.of(2026, 12, 14), null));
        groupClass.getSlots().add(slot(LocalDate.of(2026, 12, 7), "teams-1"));

        // when
        GroupClassWithSlotsResponseDTO dto = mapper.toDtoWithSlots(groupClass);

        // then
        assertThat(dto.subjectName()).isEqualTo("English B22");
        assertThat(dto.subjectCode()).isEqualTo("ENG");
        assertThat(dto.classType()).isEqualTo("language course");
        assertThat(dto.building()).isEqualTo("Main Campus");
        assertThat(dto.room()).isEqualTo("Aula A");
        assertThat(dto.startTime()).isEqualTo("08:15");
        assertThat(dto.endTime()).isEqualTo("09:45");
        assertThat(dto.regularStudentsNum()).isEqualTo(2);
        assertThat(dto.itnStudentsNum()).isEqualTo(2);
        assertThat(dto.groups()).containsExactlyInAnyOrder("WIs I.1", "WIs I.2");
        assertThat(dto.lecturers()).containsExactlyInAnyOrder("Kowalski Jan", "Nowak Anna");
        assertThat(dto.classDate()).isEqualTo("2026-12-07");
    }

    @Test
    void toDtoWithSlots_shouldReturnSlotsSortedByDate() {
        // given
        groupClass.getSlots().add(slot(LocalDate.of(2026, 12, 21), null));
        groupClass.getSlots().add(slot(LocalDate.of(2026, 12, 7), "teams-1"));
        groupClass.getSlots().add(slot(LocalDate.of(2026, 12, 14), "teams-2"));

        // when
        GroupClassWithSlotsResponseDTO dto = mapper.toDtoWithSlots(groupClass);

        // then
        assertThat(dto.slots()).containsExactly(
                new ClassSlotDTO("2026-12-07", "teams-1"),
                new ClassSlotDTO("2026-12-14", "teams-2"),
                new ClassSlotDTO("2026-12-21", null)
        );
    }

    @Test
    void toDtoWithSlots_shouldHaveNullClassDateAndNoSlots_whenClassHasNoSlots() {
        // when
        GroupClassWithSlotsResponseDTO dto = mapper.toDtoWithSlots(groupClass);

        // then
        assertThat(dto.classDate()).isNull();
        assertThat(dto.slots()).isEmpty();
    }

    @Test
    void toDtoWithSlots_shouldHaveNoLecturers_whenNoneAreAssigned() {
        // given
        groupClass.setLecturers(new HashSet<>());

        // when and then
        assertThat(mapper.toDtoWithSlots(groupClass).lecturers()).isEmpty();
    }

    // ---------------------------------------------------------------- default helpers

    @Test
    void formatClassType_shouldLowercaseAndReplaceUnderscores() {
        // when and then
        assertThat(mapper.formatClassType(SubjectType.DIPLOMA_PROJECT)).isEqualTo("diploma project");
        assertThat(mapper.formatClassType(SubjectType.LECTURE)).isEqualTo("lecture");
        assertThat(mapper.formatClassType(null)).isNull();
    }

    @Test
    void toTimeString_shouldFormatAsHoursAndMinutes() {
        // when and then
        assertThat(mapper.toTimeString(LocalTime.of(8, 5, 30))).isEqualTo("08:05");
        assertThat(mapper.toTimeString(null)).isNull();
    }

    @Test
    void toDateString_shouldUseIsoFormat() {
        // when and then
        assertThat(mapper.toDateString(LocalDate.of(2026, 1, 2))).isEqualTo("2026-01-02");
        assertThat(mapper.toDateString(null)).isNull();
    }

    @Test
    void calculateDuration_shouldReturnMinutesBetweenStartAndEnd() {
        // when and then
        assertThat(mapper.calculateDuration(groupClass)).isEqualTo(90);
        assertThat(mapper.calculateDuration(null)).isZero();

        groupClass.setEndTime(null);
        assertThat(mapper.calculateDuration(groupClass)).isZero();
    }

    @Test
    void countStudents_shouldIgnoreNonStudentMembers() {
        // given
        AcademicGroupEntity group = new AcademicGroupEntity();
        group.setName("Legacy");
        group.getMemberships().add(membership(user(Role.STUDENT), group, false));
        group.getMemberships().add(membership(user(Role.LECTURER), group, false));
        group.getMemberships().add(membership(user(Role.EMPLOYEE), group, true));
        groupClass.setGroups(new HashSet<>(Set.of(group)));

        // when and then
        assertThat(mapper.countRegularStudents(groupClass)).isEqualTo(1);
        assertThat(mapper.countItnStudents(groupClass)).isZero();
    }

    @Test
    void countStudents_shouldReturnZero_whenGroupsAreMissing() {
        // given
        groupClass.setGroups(null);

        // when and then
        assertThat(mapper.countRegularStudents(groupClass)).isZero();
        assertThat(mapper.countItnStudents(groupClass)).isZero();
        assertThat(mapper.countRegularStudents(null)).isZero();
    }

    @Test
    void extractLecturers_shouldBeEmpty_whenEntityOrLecturersAreNull() {
        // when and then
        assertThat(mapper.extractLecturers(null)).isEmpty();

        groupClass.setLecturers(null);
        assertThat(mapper.extractLecturers(groupClass)).isEmpty();
    }

    @Test
    void earliestSlotDate_andSortedSlots_shouldHandleNull() {
        // when and then
        assertThat(mapper.earliestSlotDate(null)).isNull();
        assertThat(mapper.toSortedSlotDtos(null)).isEqualTo(List.of());
    }

    // ---------------------------------------------------------------- helpers

    private ClassSlotEntity slot(LocalDate date, String msTeamsCode) {
        ClassSlotEntity slot = new ClassSlotEntity();
        slot.setGroupClass(groupClass);
        slot.setClassDate(date);
        slot.setMsTeamsCode(msTeamsCode);
        return slot;
    }

    private AcademicGroupEntity groupWithStudents(String name, int regular, int itn) {
        AcademicGroupEntity group = new AcademicGroupEntity();
        group.setName(name);
        for (int i = 0; i < regular; i++) {
            group.getMemberships().add(membership(user(Role.STUDENT), group, false));
        }
        for (int i = 0; i < itn; i++) {
            group.getMemberships().add(membership(user(Role.STUDENT), group, true));
        }
        return group;
    }

    private GroupMembershipEntity membership(UserEntity user, AcademicGroupEntity group, boolean itn) {
        return new GroupMembershipEntity(user, group, itn);
    }

    private UserEntity user(Role role) {
        UserEntity user = new UserEntity();
        user.setRole(role);
        return user;
    }

    private UserEntity lecturer(String firstName, String lastName) {
        UserEntity lecturer = user(Role.LECTURER);
        lecturer.setFirstName(firstName);
        lecturer.setLastName(lastName);
        return lecturer;
    }
}
