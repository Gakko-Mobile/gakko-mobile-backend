package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroup;
import com.gakkomobile.academicgroup.AcademicGroupRepository;
import com.gakkomobile.academicgroup.exception.AcademicGroupNotFoundException;
import com.gakkomobile.building.Building;
import com.gakkomobile.exception.BadSearchParamsException;
import com.gakkomobile.groupclass.dto.GroupClassResponse;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
import com.gakkomobile.groupmembership.GroupMembership;
import com.gakkomobile.room.Room;
import com.gakkomobile.subject.Subject;
import com.gakkomobile.subject.SubjectType;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.User;
import com.gakkomobile.user.UserRepository;
import com.gakkomobile.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupClassServiceTest {

    @Mock
    private GroupClassRepository groupClassRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AcademicGroupRepository academicGroupRepository;

    @InjectMocks
    private GroupClassService groupClassService;

    @Test
    void getDailySchedule_IndividualFilter_ShouldReturnMappedResponses() {
        LocalDate date = LocalDate.of(2026, 10, 3);
        String email = "danisa.novikova@gmail.com";
        OffsetDateTime startOfDay = date.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        when(userRepository.existsByEmail(email)).thenReturn(true);
        when(groupClassRepository.findClassesByUserEmailAndDate(email, startOfDay, endOfDay))
                .thenReturn(List.of(createMockGroupClass()));

        List<GroupClassResponse> result = groupClassService.getDailySchedule(
                ScheduleFilterOption.INDIVIDUAL, email, null, date
        );

        assertThat(result).hasSize(1);
        verifyMappingLogic(result.get(0));
        verify(groupClassRepository).findClassesByUserEmailAndDate(email, startOfDay, endOfDay);
    }

    @Test
    void getDailySchedule_IndividualFilter_ShouldThrowException_WhenEmailIsNull() {
        LocalDate date = LocalDate.now();

        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.INDIVIDUAL, null, null, date))
                .isInstanceOf(BadSearchParamsException.class);

        verifyNoInteractions(userRepository, groupClassRepository);
    }

    @Test
    void getDailySchedule_IndividualFilter_ShouldThrowException_WhenUserNotFound() {
        LocalDate date = LocalDate.now();
        String email = "unknown@gmail.com";

        when(userRepository.existsByEmail(email)).thenReturn(false);

        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.INDIVIDUAL, email, null, date))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(groupClassRepository);
    }

    @Test
    void getDailySchedule_GroupFilter_ShouldReturnMappedResponses() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 10, 3);
        UUID groupId = UUID.randomUUID();
        OffsetDateTime startOfDay = date.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        when(academicGroupRepository.existsById(groupId)).thenReturn(true);
        when(groupClassRepository.findClassesByGroupIdAndDate(groupId, startOfDay, endOfDay))
                .thenReturn(List.of(createMockGroupClass()));

        List<GroupClassResponse> result = groupClassService.getDailySchedule(
                ScheduleFilterOption.GROUP, null, groupId, date
        );

        assertThat(result).hasSize(1);
        verifyMappingLogic(result.get(0));
        verify(groupClassRepository).findClassesByGroupIdAndDate(groupId, startOfDay, endOfDay);
    }

    @Test
    void getDailySchedule_GroupFilter_ShouldThrowException_WhenGroupIdIsNull() {
        LocalDate date = LocalDate.now();

        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.GROUP, null, null, date))
                .isInstanceOf(BadSearchParamsException.class);

        verifyNoInteractions(academicGroupRepository, groupClassRepository);
    }

    @Test
    void getDailySchedule_GroupFilter_ShouldThrowException_WhenGroupNotFound() {
        LocalDate date = LocalDate.now();
        UUID groupId = UUID.randomUUID();

        when(academicGroupRepository.existsById(groupId)).thenReturn(false);

        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.GROUP, null, groupId, date))
                .isInstanceOf(AcademicGroupNotFoundException.class);

        verifyNoInteractions(groupClassRepository);
    }

    private void verifyMappingLogic(GroupClassResponse response) {
        assertThat(response.regularStudentsNum()).isEqualTo(1);
        assertThat(response.itnStudentsNum()).isEqualTo(1);
        assertThat(response.lecturers()).containsExactly("Kowalski Jan");
        assertThat(response.groups()).containsExactly("WIs I.1");
        assertThat(response.classType()).isEqualTo("language course");
        assertThat(response.durationInMins()).isEqualTo(90);
        assertThat(response.startTime()).isEqualTo("08:15");
        assertThat(response.endTime()).isEqualTo("09:45");
        assertThat(response.room()).isEqualTo("Aula A");
        assertThat(response.building()).isEqualTo("Main Campus");
        assertThat(response.msTeamsCode()).isEqualTo("teams-123");
    }

    private GroupClass createMockGroupClass() {
        Building building = new Building();
        building.setName("Main Campus");

        Room room = new Room();
        room.setName("Aula A");
        room.setBuilding(building);

        Subject subject = new Subject();
        subject.setName("English B22");
        subject.setCode("ENG");
        subject.setType(SubjectType.LANGUAGE_COURSE);

        User regularStudent = new User();
        regularStudent.setRole(Role.STUDENT);
        GroupMembership regMem = new GroupMembership();
        regMem.setUser(regularStudent);
        regMem.setItn(false);

        User itnStudent = new User();
        itnStudent.setRole(Role.STUDENT);
        GroupMembership itnMem = new GroupMembership();
        itnMem.setUser(itnStudent);
        itnMem.setItn(true);

        User lecturer = new User();
        lecturer.setRole(Role.LECTURER);
        lecturer.setFirstName("Jan");
        lecturer.setLastName("Kowalski");
        GroupMembership lecMem = new GroupMembership();
        lecMem.setUser(lecturer);
        lecMem.setItn(false);

        AcademicGroup group = new AcademicGroup();
        group.setName("WIs I.1");
        group.setMemberships(Set.of(regMem, itnMem, lecMem));

        GroupClass gc = new GroupClass();
        gc.setRoom(room);
        gc.setSubject(subject);
        gc.setGroups(Set.of(group));
        gc.setStartTimestamp(OffsetDateTime.of(2026, 10, 3, 8, 15, 0, 0, ZoneOffset.UTC));
        gc.setEndTimestamp(OffsetDateTime.of(2026, 10, 3, 9, 45, 0, 0, ZoneOffset.UTC));
        gc.setMsTeamsCode("teams-123");

        return gc;
    }
}