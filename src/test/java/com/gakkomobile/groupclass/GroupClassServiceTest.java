package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupService;
import com.gakkomobile.academicgroup.exception.AcademicGroupNotFoundException;
import com.gakkomobile.exception.BadSearchParamsException;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
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
    private AcademicGroupService academicGroupService;
    @Mock
    private GroupClassMapper groupClassMapper;

    @InjectMocks
    private GroupClassService groupClassService;

    @Test
    void getDailySchedule_IndividualFilter_ShouldReturnMappedResponses() {
        // given
        LocalDate date = LocalDate.of(2026, 10, 3);
        String email = "danisa.novikova@gmail.com";
        OffsetDateTime startOfDay = date.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        GroupClassEntity entity = new GroupClassEntity();
        GroupClassResponseDTO expectedDto = createMockDto();

        when(userRepository.existsByEmail(email)).thenReturn(true);
        when(groupClassRepository.findClassesByUserEmailAndDate(email, startOfDay, endOfDay))
                .thenReturn(List.of(entity));
        when(groupClassMapper.toDto(entity)).thenReturn(expectedDto);

        // when
        List<GroupClassResponseDTO> result = groupClassService.getDailySchedule(
                ScheduleFilterOption.INDIVIDUAL, email, null, date
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(expectedDto);

        verify(groupClassRepository).findClassesByUserEmailAndDate(email, startOfDay, endOfDay);
        verify(groupClassMapper).toDto(entity);
    }

    @Test
    void getDailySchedule_IndividualFilter_ShouldThrowException_WhenEmailIsNull() {
        // given
        LocalDate date = LocalDate.now();

        // when and then
        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.INDIVIDUAL, null, null, date))
                .isInstanceOf(BadSearchParamsException.class);

        verifyNoInteractions(userRepository, groupClassRepository, groupClassMapper);
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

        verifyNoInteractions(groupClassRepository, groupClassMapper);
    }

    @Test
    void getDailySchedule_GroupFilter_ShouldReturnMappedResponses() {
        // given
        LocalDate date = LocalDate.of(2026, 10, 3);
        UUID groupId = UUID.randomUUID();
        OffsetDateTime startOfDay = date.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        GroupClassEntity entity = new GroupClassEntity();
        GroupClassResponseDTO expectedDto = createMockDto();

        when(academicGroupService.existsById(groupId)).thenReturn(true);
        when(groupClassRepository.findClassesByGroupIdAndDate(groupId, startOfDay, endOfDay))
                .thenReturn(List.of(entity));
        when(groupClassMapper.toDto(entity)).thenReturn(expectedDto);

        // when
        List<GroupClassResponseDTO> result = groupClassService.getDailySchedule(
                ScheduleFilterOption.GROUP, null, groupId, date
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(expectedDto);

        verify(groupClassRepository).findClassesByGroupIdAndDate(groupId, startOfDay, endOfDay);
        verify(groupClassMapper).toDto(entity);
    }

    @Test
    void getDailySchedule_GroupFilter_ShouldThrowException_WhenGroupIdIsNull() {
        // given
        LocalDate date = LocalDate.now();

        // when and then
        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.GROUP, null, null, date))
                .isInstanceOf(BadSearchParamsException.class);

        verifyNoInteractions(academicGroupService, groupClassRepository, groupClassMapper);
    }

    @Test
    void getDailySchedule_GroupFilter_ShouldThrowException_WhenGroupNotFound() {
        // given
        LocalDate date = LocalDate.now();
        UUID groupId = UUID.randomUUID();

        when(academicGroupService.existsById(groupId)).thenReturn(false);

        // when and then
        assertThatThrownBy(() -> groupClassService.getDailySchedule(
                ScheduleFilterOption.GROUP, null, groupId, date))
                .isInstanceOf(AcademicGroupNotFoundException.class);

        verifyNoInteractions(groupClassRepository, groupClassMapper);
    }

    private GroupClassResponseDTO createMockDto() {
        return new GroupClassResponseDTO(
                1,
                1,
                "English B22",
                "ENG",
                "language course",
                Set.of("WIs I.1"),
                Set.of("Kowalski Jan"),
                "Main Campus",
                "Aula A",
                "2026-10-03",
                "08:15",
                "09:45",
                90,
                "teams-123"
        );
    }
}