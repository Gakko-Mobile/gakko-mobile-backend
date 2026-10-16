package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupEntity;
import com.gakkomobile.academicgroup.AcademicGroupRepository;
import com.gakkomobile.academicgroup.exception.AcademicGroupNotFoundException;
import com.gakkomobile.building.BuildingEntity;
import com.gakkomobile.exception.BadSearchParamsException;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
import com.gakkomobile.groupmembership.GroupMembershipEntity;
import com.gakkomobile.room.RoomEntity;
import com.gakkomobile.subject.SubjectEntity;
import com.gakkomobile.subject.SubjectType;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
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
        // given
        LocalDate date = LocalDate.of(2026, 10, 3);
        String email = "danisa.novikova@gmail.com";
        OffsetDateTime startOfDay = date.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        when(userRepository.existsByEmail(email)).thenReturn(true);
        when(groupClassRepository.findClassesByUserEmailAndDate(email, startOfDay, endOfDay))
                .thenReturn(List.of(createMockGroupClass()));

        // when
        List<GroupClassResponseDTO> result = groupClassService.getDailySchedule(
                ScheduleFilterOption.INDIVIDUAL, email, null, date
        );

        // then
        assertThat(result).hasSize(1);
        verifyMappingLogic(result.get(0));
        verify(groupClassRepository).findClassesByUserEmailAndDate(email, startOfDay, endOfDay);
    }

    @Test
    void getDailySchedule_IndividualFilter_ShouldThrowException_WhenEmailIsNull() {
        // given
        LocalDate date = LocalDate.now();

        // when and then
        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.INDIVIDUAL, null, null, date))
                .isInstanceOf(BadSearchParamsException.class);

        verifyNoInteractions(userRepository, groupClassRepository);
    }

    @Test
    void getDailySchedule_IndividualFilter_ShouldThrowException_WhenUserNotFound() {
        // given
        LocalDate date = LocalDate.now();
        String email = "unknown@gmail.com";

        when(userRepository.existsByEmail(email)).thenReturn(false);

        // when and then
        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.INDIVIDUAL, email, null, date))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(groupClassRepository);
    }

    @Test
    void getDailySchedule_GroupFilter_ShouldReturnMappedResponses() {
        // given
        LocalDate date = LocalDate.of(2026, 10, 3);
        UUID groupId = UUID.randomUUID();
        OffsetDateTime startOfDay = date.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        when(academicGroupRepository.existsById(groupId)).thenReturn(true);
        when(groupClassRepository.findClassesByGroupIdAndDate(groupId, startOfDay, endOfDay))
                .thenReturn(List.of(createMockGroupClass()));

        // when
        List<GroupClassResponseDTO> result = groupClassService.getDailySchedule(
                ScheduleFilterOption.GROUP, null, groupId, date
        );

        // then
        assertThat(result).hasSize(1);
        verifyMappingLogic(result.get(0));
        verify(groupClassRepository).findClassesByGroupIdAndDate(groupId, startOfDay, endOfDay);
    }

    @Test
    void getDailySchedule_GroupFilter_ShouldThrowException_WhenGroupIdIsNull() {
        // given
        LocalDate date = LocalDate.now();

        // when and then
        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.GROUP, null, null, date))
                .isInstanceOf(BadSearchParamsException.class);

        verifyNoInteractions(academicGroupRepository, groupClassRepository);
    }

    @Test
    void getDailySchedule_GroupFilter_ShouldThrowException_WhenGroupNotFound() {
        // given
        LocalDate date = LocalDate.now();
        UUID groupId = UUID.randomUUID();

        when(academicGroupRepository.existsById(groupId)).thenReturn(false);

        // when and then
        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.GROUP, null, groupId, date))
                .isInstanceOf(AcademicGroupNotFoundException.class);

        verifyNoInteractions(groupClassRepository);
    }

    private void verifyMappingLogic(GroupClassResponseDTO response) {
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

    private GroupClassEntity createMockGroupClass() {
        BuildingEntity building = new BuildingEntity();
        building.setName("Main Campus");

        RoomEntity room = new RoomEntity();
        room.setName("Aula A");
        room.setBuilding(building);

        SubjectEntity subject = new SubjectEntity();
        subject.setName("English B22");
        subject.setCode("ENG");
        subject.setType(SubjectType.LANGUAGE_COURSE);

        UserEntity regularStudent = new UserEntity();
        regularStudent.setRole(Role.STUDENT);
        GroupMembershipEntity regMem = new GroupMembershipEntity();
        regMem.setUser(regularStudent);
        regMem.setItn(false);

        UserEntity itnStudent = new UserEntity();
        itnStudent.setRole(Role.STUDENT);
        GroupMembershipEntity itnMem = new GroupMembershipEntity();
        itnMem.setUser(itnStudent);
        itnMem.setItn(true);

        UserEntity lecturer = new UserEntity();
        lecturer.setRole(Role.LECTURER);
        lecturer.setFirstName("Jan");
        lecturer.setLastName("Kowalski");
        GroupMembershipEntity lecMem = new GroupMembershipEntity();
        lecMem.setUser(lecturer);
        lecMem.setItn(false);

        AcademicGroupEntity group = new AcademicGroupEntity();
        group.setName("WIs I.1");
        group.setMemberships(Set.of(regMem, itnMem, lecMem));

        GroupClassEntity gc = new GroupClassEntity();
        gc.setRoom(room);
        gc.setSubject(subject);
        gc.setGroups(Set.of(group));
        gc.setStartTimestamp(OffsetDateTime.of(2026, 10, 3, 8, 15, 0, 0, ZoneOffset.UTC));
        gc.setEndTimestamp(OffsetDateTime.of(2026, 10, 3, 9, 45, 0, 0, ZoneOffset.UTC));
        gc.setMsTeamsCode("teams-123");

        return gc;
    }
}