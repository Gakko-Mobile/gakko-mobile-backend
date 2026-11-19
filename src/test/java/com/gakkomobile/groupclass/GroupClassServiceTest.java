package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupEntity;
import com.gakkomobile.academicgroup.AcademicGroupService;
import com.gakkomobile.academicgroup.exception.AcademicGroupNotFoundException;
import com.gakkomobile.exception.BadSearchParamsException;
import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.groupclass.dto.ClassSlotDTO;
import com.gakkomobile.groupclass.dto.CreateClassSlotRequestDTO;
import com.gakkomobile.groupclass.dto.CreateGroupClassRequestDTO;
import com.gakkomobile.groupclass.dto.CreateScheduleRequestDTO;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.dto.GroupClassWithSlotsResponseDTO;
import com.gakkomobile.groupclass.dto.ScheduleGroupClassDTO;
import com.gakkomobile.groupclass.dto.UpdateClassSlotRequestDTO;
import com.gakkomobile.groupclass.dto.UpdateGroupClassRequestDTO;
import com.gakkomobile.groupclass.exception.ClassSlotConflictException;
import com.gakkomobile.groupclass.exception.InvalidScheduleException;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
import com.gakkomobile.room.RoomEntity;
import com.gakkomobile.room.RoomService;
import com.gakkomobile.subject.SubjectEntity;
import com.gakkomobile.subject.SubjectService;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserService;
import com.gakkomobile.user.exception.NotALecturerException;
import com.gakkomobile.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupClassServiceTest {

    private static final LocalTime START = LocalTime.of(8, 15);
    private static final LocalTime END = LocalTime.of(9, 45);

    @Mock
    private GroupClassRepository groupClassRepository;
    @Mock
    private ClassSlotRepository classSlotRepository;
    @Mock
    private AcademicGroupService academicGroupService;
    @Mock
    private GroupClassMapper groupClassMapper;
    @Mock
    private SubjectService subjectService;
    @Mock
    private RoomService roomService;
    @Mock
    private UserService userService;

    @InjectMocks
    private GroupClassService groupClassService;

    @Captor
    private ArgumentCaptor<List<GroupClassEntity>> groupClassListCaptor;
    @Captor
    private ArgumentCaptor<GroupClassEntity> groupClassCaptor;
    @Captor
    private ArgumentCaptor<ClassSlotEntity> slotCaptor;

    private final UUID subjectId = UUID.randomUUID();
    private final UUID roomId = UUID.randomUUID();
    private final UUID groupId = UUID.randomUUID();
    private final UUID lecturerId = UUID.randomUUID();

    private final SubjectEntity subject = new SubjectEntity();
    private final RoomEntity room = new RoomEntity();
    private final Set<AcademicGroupEntity> groups = Set.of(new AcademicGroupEntity());
    private final Set<UserEntity> lecturers = Set.of(new UserEntity());

    // ---------------------------------------------------------------- getDailySchedule

    @Nested
    class GetDailySchedule {

        @Test
        void individualFilter_shouldReturnMappedResponses() {
            // given
            LocalDate date = LocalDate.of(2026, 10, 3);
            String email = "danisa.novikova@gmail.com";
            ClassSlotEntity slot = new ClassSlotEntity();
            GroupClassResponseDTO expectedDto = dailyDto();

            when(classSlotRepository.findSlotsByUserEmailAndDate(email, date)).thenReturn(List.of(slot));
            when(groupClassMapper.toDto(slot)).thenReturn(expectedDto);

            // when
            List<GroupClassResponseDTO> result = groupClassService.getDailySchedule(
                    ScheduleFilterOption.INDIVIDUAL, email, null, date
            );

            // then
            assertThat(result).containsExactly(expectedDto);
            verify(userService).existsByEmail(email);
            verify(classSlotRepository).findSlotsByUserEmailAndDate(email, date);
        }

        @Test
        void individualFilter_shouldThrow_whenEmailIsNull() {
            // given
            LocalDate date = LocalDate.now();

            // when and then
            assertThatThrownBy(() -> groupClassService.getDailySchedule(
                    ScheduleFilterOption.INDIVIDUAL, null, null, date))
                    .isInstanceOf(BadSearchParamsException.class)
                    .hasMessage(ExceptionErrorConstants.EMAIL_NOT_PROVIDED);

            verifyNoInteractions(userService, classSlotRepository, groupClassMapper);
        }

        @Test
        void individualFilter_shouldThrow_whenUserNotFound() {
            // given
            LocalDate date = LocalDate.now();
            String email = "unknown@gmail.com";

            doThrow(new UserNotFoundException()).when(userService).existsByEmail(email);

            // when and then
            assertThatThrownBy(() -> groupClassService.getDailySchedule(
                    ScheduleFilterOption.INDIVIDUAL, email, null, date))
                    .isInstanceOf(UserNotFoundException.class);

            verifyNoInteractions(classSlotRepository, groupClassMapper);
        }

        @Test
        void groupFilter_shouldReturnMappedResponses() {
            // given
            LocalDate date = LocalDate.of(2026, 10, 3);
            ClassSlotEntity slot = new ClassSlotEntity();
            GroupClassResponseDTO expectedDto = dailyDto();

            when(academicGroupService.existsById(groupId)).thenReturn(true);
            when(classSlotRepository.findSlotsByGroupIdAndDate(groupId, date)).thenReturn(List.of(slot));
            when(groupClassMapper.toDto(slot)).thenReturn(expectedDto);

            // when
            List<GroupClassResponseDTO> result = groupClassService.getDailySchedule(
                    ScheduleFilterOption.GROUP, null, groupId, date
            );

            // then
            assertThat(result).containsExactly(expectedDto);
            verify(classSlotRepository).findSlotsByGroupIdAndDate(groupId, date);
        }

        @Test
        void groupFilter_shouldThrow_whenGroupIdIsNull() {
            // given
            LocalDate date = LocalDate.now();

            // when and then
            assertThatThrownBy(() -> groupClassService.getDailySchedule(
                    ScheduleFilterOption.GROUP, null, null, date))
                    .isInstanceOf(BadSearchParamsException.class);

            verifyNoInteractions(academicGroupService, classSlotRepository, groupClassMapper);
        }

        @Test
        void groupFilter_shouldThrow_whenGroupNotFound() {
            // given
            LocalDate date = LocalDate.now();

            when(academicGroupService.existsById(groupId)).thenReturn(false);

            // when and then
            assertThatThrownBy(() -> groupClassService.getDailySchedule(
                    ScheduleFilterOption.GROUP, null, groupId, date))
                    .isInstanceOf(AcademicGroupNotFoundException.class);

            verifyNoInteractions(classSlotRepository, groupClassMapper);
        }
    }

    // ---------------------------------------------------------------- createSchedule

    @Nested
    class CreateSchedule {

        @Test
        void shouldCreateOneRecurringClassPerEntryWithWeeklySlots() {
            // given
            stubReferenceData();
            stubSaveAllAndMapper();
            // Monday 2026-11-23 .. Sunday 2026-12-13 -> 3 Mondays and 3 Thursdays
            CreateScheduleRequestDTO request = new CreateScheduleRequestDTO(
                    LocalDate.of(2026, 11, 23),
                    LocalDate.of(2026, 12, 13),
                    Set.of(
                            scheduleEntry(DayOfWeek.MONDAY, LocalTime.of(8, 15), LocalTime.of(9, 45)),
                            scheduleEntry(DayOfWeek.THURSDAY, LocalTime.of(13, 30), LocalTime.of(15, 0))
                    )
            );

            // when
            List<GroupClassWithSlotsResponseDTO> result = groupClassService.createSchedule(request);

            // then
            verify(groupClassRepository).saveAll(groupClassListCaptor.capture());
            List<GroupClassEntity> saved = groupClassListCaptor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved).allSatisfy(groupClass -> {
                assertThat(groupClass.isOneTime()).isFalse();
                assertThat(groupClass.getSubject()).isSameAs(subject);
                assertThat(groupClass.getRoom()).isSameAs(room);
                assertThat(groupClass.getGroups()).containsExactlyElementsOf(groups);
                assertThat(groupClass.getLecturers()).containsExactlyElementsOf(lecturers);
                assertThat(groupClass.getSlots()).hasSize(3);
                assertThat(groupClass.getSlots()).allSatisfy(slot -> {
                    assertThat(slot.getGroupClass()).isSameAs(groupClass);
                    assertThat(slot.getMsTeamsCode()).isNull();
                });
            });

            GroupClassEntity monday = findByStartTime(saved, LocalTime.of(8, 15));
            assertThat(slotDates(monday)).containsExactly(
                    LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 7));
            assertThat(monday.getEndTime()).isEqualTo(LocalTime.of(9, 45));

            GroupClassEntity thursday = findByStartTime(saved, LocalTime.of(13, 30));
            assertThat(slotDates(thursday)).containsExactly(
                    LocalDate.of(2026, 11, 26), LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 10));

            assertThat(result).hasSize(2);
        }

        @Test
        void shouldReturnClassesSortedByFirstDateThenStartTime() {
            // given
            stubReferenceData();
            stubSaveAllAndMapper();
            CreateScheduleRequestDTO request = new CreateScheduleRequestDTO(
                    LocalDate.of(2026, 11, 23),
                    LocalDate.of(2026, 11, 30),
                    Set.of(
                            scheduleEntry(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)),
                            scheduleEntry(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(15, 0)),
                            scheduleEntry(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0))
                    )
            );

            // when
            List<GroupClassWithSlotsResponseDTO> result = groupClassService.createSchedule(request);

            // then
            assertThat(result)
                    .extracting(GroupClassWithSlotsResponseDTO::classDate, GroupClassWithSlotsResponseDTO::startTime)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("2026-11-23", "10:00"),
                            org.assertj.core.groups.Tuple.tuple("2026-11-23", "14:00"),
                            org.assertj.core.groups.Tuple.tuple("2026-11-25", "08:00")
                    );
        }

        @Test
        void shouldStartOnTheFirstMatchingWeekDayAfterTheStartDate() {
            // given
            stubReferenceData();
            stubSaveAllAndMapper();
            // Tuesday start: the first Monday is 2026-11-30
            CreateScheduleRequestDTO request = new CreateScheduleRequestDTO(
                    LocalDate.of(2026, 11, 24),
                    LocalDate.of(2026, 12, 8),
                    Set.of(scheduleEntry(DayOfWeek.MONDAY, START, END))
            );

            // when
            groupClassService.createSchedule(request);

            // then
            verify(groupClassRepository).saveAll(groupClassListCaptor.capture());
            assertThat(slotDates(groupClassListCaptor.getValue().get(0))).containsExactly(
                    LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 7));
        }

        @Test
        void shouldIncludeBothBoundaryDates() {
            // given
            stubReferenceData();
            stubSaveAllAndMapper();
            // Monday start and Monday end: both are included
            CreateScheduleRequestDTO request = new CreateScheduleRequestDTO(
                    LocalDate.of(2026, 11, 23),
                    LocalDate.of(2026, 11, 30),
                    Set.of(scheduleEntry(DayOfWeek.MONDAY, START, END))
            );

            // when
            groupClassService.createSchedule(request);

            // then
            verify(groupClassRepository).saveAll(groupClassListCaptor.capture());
            assertThat(slotDates(groupClassListCaptor.getValue().get(0))).containsExactly(
                    LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 30));
        }

        @Test
        void shouldAllowTheMaximumRangeOfFiveMonths() {
            // given
            stubReferenceData();
            stubSaveAllAndMapper();
            LocalDate start = LocalDate.of(2026, 11, 23);
            CreateScheduleRequestDTO request = new CreateScheduleRequestDTO(
                    start,
                    start.plusMonths(GroupClassService.MAX_SCHEDULE_RANGE_IN_MONTHS),
                    Set.of(scheduleEntry(DayOfWeek.MONDAY, START, END))
            );

            // when and then
            assertThat(groupClassService.createSchedule(request)).hasSize(1);
        }

        @Test
        void shouldLoadSharedReferenceDataOnlyOnce() {
            // given
            stubReferenceData();
            stubSaveAllAndMapper();
            CreateScheduleRequestDTO request = new CreateScheduleRequestDTO(
                    LocalDate.of(2026, 11, 23),
                    LocalDate.of(2026, 12, 13),
                    Set.of(
                            scheduleEntry(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)),
                            scheduleEntry(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)),
                            scheduleEntry(DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(9, 0))
                    )
            );

            // when
            groupClassService.createSchedule(request);

            // then
            verify(subjectService, times(1)).getById(subjectId);
            verify(roomService, times(1)).getById(roomId);
            verify(academicGroupService, times(1)).getAllByIds(Set.of(groupId));
            verify(userService, times(1)).getLecturersByIds(Set.of(lecturerId));
        }

        @Test
        void shouldRejectEndDateBeforeStartDate() {
            // given
            CreateScheduleRequestDTO request = new CreateScheduleRequestDTO(
                    LocalDate.of(2026, 12, 1),
                    LocalDate.of(2026, 11, 1),
                    Set.of(scheduleEntry(DayOfWeek.MONDAY, START, END))
            );

            // when and then
            assertThatThrownBy(() -> groupClassService.createSchedule(request))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessage(ExceptionErrorConstants.SCHEDULE_END_DATE_BEFORE_START_DATE);

            verifyNothingLoadedOrSaved();
        }

        @Test
        void shouldRejectRangeLongerThanFiveMonths() {
            // given
            LocalDate start = LocalDate.of(2026, 11, 23);
            CreateScheduleRequestDTO request = new CreateScheduleRequestDTO(
                    start,
                    start.plusMonths(GroupClassService.MAX_SCHEDULE_RANGE_IN_MONTHS).plusDays(1),
                    Set.of(scheduleEntry(DayOfWeek.MONDAY, START, END))
            );

            // when and then
            assertThatThrownBy(() -> groupClassService.createSchedule(request))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessage(ExceptionErrorConstants.SCHEDULE_DATE_RANGE_TOO_LONG.formatted(
                            GroupClassService.MAX_SCHEDULE_RANGE_IN_MONTHS));

            verifyNothingLoadedOrSaved();
        }

        @Test
        void shouldRejectEntryWhoseEndTimeIsNotAfterStartTime() {
            // given
            CreateScheduleRequestDTO request = new CreateScheduleRequestDTO(
                    LocalDate.of(2026, 11, 23),
                    LocalDate.of(2026, 12, 13),
                    Set.of(scheduleEntry(DayOfWeek.MONDAY, END, START))
            );

            // when and then
            assertThatThrownBy(() -> groupClassService.createSchedule(request))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessage(ExceptionErrorConstants.SCHEDULE_SLOT_END_TIME_NOT_AFTER_START_TIME);

            verifyNothingLoadedOrSaved();
        }

        @Test
        void shouldPropagateNotALecturerAndSaveNothing() {
            // given
            when(subjectService.getById(subjectId)).thenReturn(subject);
            when(roomService.getById(roomId)).thenReturn(room);
            when(academicGroupService.getAllByIds(Set.of(groupId))).thenReturn(groups);
            when(userService.getLecturersByIds(Set.of(lecturerId)))
                    .thenThrow(new NotALecturerException("not a lecturer"));
            CreateScheduleRequestDTO request = new CreateScheduleRequestDTO(
                    LocalDate.of(2026, 11, 23),
                    LocalDate.of(2026, 12, 13),
                    Set.of(scheduleEntry(DayOfWeek.MONDAY, START, END))
            );

            // when and then
            assertThatThrownBy(() -> groupClassService.createSchedule(request))
                    .isInstanceOf(NotALecturerException.class);

            verify(groupClassRepository, never()).saveAll(anyList());
        }

        private void verifyNothingLoadedOrSaved() {
            verifyNoInteractions(
                    subjectService, roomService, academicGroupService, userService, groupClassRepository
            );
        }

        private void stubSaveAllAndMapper() {
            when(groupClassRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
            when(groupClassMapper.toDtoWithSlots(any(GroupClassEntity.class)))
                    .thenAnswer(invocation -> withSlotsDto(invocation.getArgument(0)));
        }
    }

    // ---------------------------------------------------------------- createGroupClass

    @Nested
    class CreateGroupClass {

        @Test
        void shouldCreateOneTimeClassWithSingleSlot() {
            // given
            stubReferenceData();
            when(groupClassRepository.save(any(GroupClassEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(groupClassMapper.toDtoWithSlots(any(GroupClassEntity.class)))
                    .thenAnswer(invocation -> withSlotsDto(invocation.getArgument(0)));
            CreateGroupClassRequestDTO request = createRequest(
                    true, List.of(new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 14), "teams-abc")));

            // when
            GroupClassWithSlotsResponseDTO result = groupClassService.createGroupClass(request);

            // then
            verify(groupClassRepository).save(groupClassCaptor.capture());
            GroupClassEntity saved = groupClassCaptor.getValue();
            assertThat(saved.isOneTime()).isTrue();
            assertThat(saved.getSubject()).isSameAs(subject);
            assertThat(saved.getRoom()).isSameAs(room);
            assertThat(saved.getGroups()).containsExactlyElementsOf(groups);
            assertThat(saved.getLecturers()).containsExactlyElementsOf(lecturers);
            assertThat(saved.getStartTime()).isEqualTo(START);
            assertThat(saved.getEndTime()).isEqualTo(END);
            assertThat(saved.getSlots()).hasSize(1);
            ClassSlotEntity slot = saved.getSlots().iterator().next();
            assertThat(slot.getClassDate()).isEqualTo(LocalDate.of(2026, 12, 14));
            assertThat(slot.getMsTeamsCode()).isEqualTo("teams-abc");
            assertThat(slot.getGroupClass()).isSameAs(saved);
            assertThat(result.slots()).containsExactly(new ClassSlotDTO("2026-12-14", "teams-abc"));
        }

        @Test
        void shouldCreateRecurringClassWithManySlots() {
            // given
            stubReferenceData();
            when(groupClassRepository.save(any(GroupClassEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(groupClassMapper.toDtoWithSlots(any(GroupClassEntity.class)))
                    .thenAnswer(invocation -> withSlotsDto(invocation.getArgument(0)));
            CreateGroupClassRequestDTO request = createRequest(false, List.of(
                    new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 21), null),
                    new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 7), null),
                    new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 14), "t3")
            ));

            // when
            GroupClassWithSlotsResponseDTO result = groupClassService.createGroupClass(request);

            // then
            verify(groupClassRepository).save(groupClassCaptor.capture());
            assertThat(groupClassCaptor.getValue().isOneTime()).isFalse();
            assertThat(groupClassCaptor.getValue().getSlots()).hasSize(3);
            assertThat(result.slots()).extracting(ClassSlotDTO::date)
                    .containsExactly("2026-12-07", "2026-12-14", "2026-12-21");
        }

        @Test
        void shouldRejectOneTimeClassWithMoreThanOneSlot() {
            // given
            CreateGroupClassRequestDTO request = createRequest(true, List.of(
                    new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 14), null),
                    new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 21), null)
            ));

            // when and then
            assertThatThrownBy(() -> groupClassService.createGroupClass(request))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessage(ExceptionErrorConstants.ONE_TIME_CLASS_MULTIPLE_SLOTS);

            verifyNoInteractions(subjectService, roomService, academicGroupService, userService, groupClassRepository);
        }

        @Test
        void shouldRejectDuplicateSlotDates() {
            // given
            CreateGroupClassRequestDTO request = createRequest(false, List.of(
                    new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 14), null),
                    new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 14), "other")
            ));

            // when and then
            assertThatThrownBy(() -> groupClassService.createGroupClass(request))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessage(ExceptionErrorConstants.DUPLICATE_SLOT_DATES);

            verifyNoInteractions(groupClassRepository);
        }

        @Test
        void shouldRejectEndTimeNotAfterStartTime() {
            // given
            CreateGroupClassRequestDTO request = new CreateGroupClassRequestDTO(
                    subjectId, roomId, Set.of(groupId), Set.of(lecturerId),
                    START, START, false,
                    List.of(new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 14), null))
            );

            // when and then
            assertThatThrownBy(() -> groupClassService.createGroupClass(request))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessage(ExceptionErrorConstants.SCHEDULE_SLOT_END_TIME_NOT_AFTER_START_TIME);

            verifyNoInteractions(groupClassRepository);
        }

        @Test
        void shouldNotSaveWhenLecturerValidationFails() {
            // given
            when(subjectService.getById(subjectId)).thenReturn(subject);
            when(roomService.getById(roomId)).thenReturn(room);
            when(academicGroupService.getAllByIds(Set.of(groupId))).thenReturn(groups);
            when(userService.getLecturersByIds(Set.of(lecturerId)))
                    .thenThrow(new NotALecturerException("not a lecturer"));
            CreateGroupClassRequestDTO request = createRequest(
                    true, List.of(new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 14), null)));

            // when and then
            assertThatThrownBy(() -> groupClassService.createGroupClass(request))
                    .isInstanceOf(NotALecturerException.class);

            verify(groupClassRepository, never()).save(any());
        }

        @Test
        void shouldPropagateUnknownSubject() {
            // given
            when(subjectService.getById(subjectId))
                    .thenThrow(new ResourceNotFoundException(ExceptionErrorConstants.SUBJECT_NOT_FOUND));
            CreateGroupClassRequestDTO request = createRequest(
                    true, List.of(new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 14), null)));

            // when and then
            assertThatThrownBy(() -> groupClassService.createGroupClass(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(ExceptionErrorConstants.SUBJECT_NOT_FOUND);

            verify(groupClassRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------- updateGroupClass

    @Nested
    class UpdateGroupClass {

        @Test
        void shouldReplaceDetailsAndKeepSlotsAndType() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false, LocalDate.of(2026, 12, 7));
            ClassSlotEntity existingSlot = existing.getSlots().iterator().next();
            existingSlot.setMsTeamsCode("keep-me");
            SubjectEntity newSubject = new SubjectEntity();
            RoomEntity newRoom = new RoomEntity();
            Set<AcademicGroupEntity> newGroups = Set.of(new AcademicGroupEntity(), new AcademicGroupEntity());
            Set<UserEntity> newLecturers = Set.of(new UserEntity());
            UUID newSubjectId = UUID.randomUUID();
            UUID newRoomId = UUID.randomUUID();
            UUID newGroupId = UUID.randomUUID();
            UUID newLecturerId = UUID.randomUUID();

            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(subjectService.getById(newSubjectId)).thenReturn(newSubject);
            when(roomService.getById(newRoomId)).thenReturn(newRoom);
            when(academicGroupService.getAllByIds(Set.of(newGroupId))).thenReturn(newGroups);
            when(userService.getLecturersByIds(Set.of(newLecturerId))).thenReturn(newLecturers);
            when(groupClassMapper.toDtoWithSlots(existing)).thenAnswer(invocation -> withSlotsDto(existing));

            // when
            GroupClassWithSlotsResponseDTO result = groupClassService.updateGroupClass(
                    classId,
                    new UpdateGroupClassRequestDTO(
                            newSubjectId, newRoomId, Set.of(newGroupId), Set.of(newLecturerId),
                            LocalTime.of(10, 0), LocalTime.of(11, 30)
                    )
            );

            // then
            assertThat(existing.getSubject()).isSameAs(newSubject);
            assertThat(existing.getRoom()).isSameAs(newRoom);
            assertThat(existing.getGroups()).isEqualTo(newGroups);
            assertThat(existing.getLecturers()).isEqualTo(newLecturers);
            assertThat(existing.getStartTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(existing.getEndTime()).isEqualTo(LocalTime.of(11, 30));
            assertThat(existing.isOneTime()).isFalse();
            assertThat(existing.getSlots()).containsExactly(existingSlot);
            assertThat(existingSlot.getMsTeamsCode()).isEqualTo("keep-me");
            assertThat(result.startTime()).isEqualTo("10:00");
        }

        @Test
        void shouldThrowWhenClassDoesNotExist() {
            // given
            UUID classId = UUID.randomUUID();
            when(groupClassRepository.findById(classId)).thenReturn(Optional.empty());

            // when and then
            assertThatThrownBy(() -> groupClassService.updateGroupClass(classId, updateRequest(START, END)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(ExceptionErrorConstants.GROUP_CLASS_NOT_FOUND);

            verifyNoInteractions(subjectService, roomService, academicGroupService, userService);
        }

        @Test
        void shouldRejectEndTimeNotAfterStartTimeAndLeaveClassUnchanged() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false, LocalDate.of(2026, 12, 7));
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));

            // when and then
            assertThatThrownBy(() -> groupClassService.updateGroupClass(classId, updateRequest(END, START)))
                    .isInstanceOf(InvalidScheduleException.class)
                    .hasMessage(ExceptionErrorConstants.SCHEDULE_SLOT_END_TIME_NOT_AFTER_START_TIME);

            assertThat(existing.getStartTime()).isEqualTo(START);
            assertThat(existing.getEndTime()).isEqualTo(END);
            verifyNoInteractions(subjectService, roomService, academicGroupService, userService);
        }

        @Test
        void shouldPropagateNotALecturer() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false, LocalDate.of(2026, 12, 7));
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(subjectService.getById(subjectId)).thenReturn(subject);
            when(roomService.getById(roomId)).thenReturn(room);
            when(academicGroupService.getAllByIds(Set.of(groupId))).thenReturn(groups);
            when(userService.getLecturersByIds(Set.of(lecturerId)))
                    .thenThrow(new NotALecturerException("not a lecturer"));

            // when and then
            assertThatThrownBy(() -> groupClassService.updateGroupClass(classId, updateRequest(START, END)))
                    .isInstanceOf(NotALecturerException.class);
        }
    }

    // ---------------------------------------------------------------- deleteGroupClass

    @Nested
    class DeleteGroupClass {

        @Test
        void shouldDeleteExistingClass() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false, LocalDate.of(2026, 12, 7));
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));

            // when
            groupClassService.deleteGroupClass(classId);

            // then
            verify(groupClassRepository).delete(existing);
        }

        @Test
        void shouldThrowWhenClassDoesNotExist() {
            // given
            UUID classId = UUID.randomUUID();
            when(groupClassRepository.findById(classId)).thenReturn(Optional.empty());

            // when and then
            assertThatThrownBy(() -> groupClassService.deleteGroupClass(classId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(ExceptionErrorConstants.GROUP_CLASS_NOT_FOUND);

            verify(groupClassRepository, never()).delete(any());
        }
    }

    // ---------------------------------------------------------------- createClassSlot

    @Nested
    class CreateClassSlot {

        @Test
        void shouldAddSlotToRecurringClassWithoutCheckingTheOneTimeRule() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false, LocalDate.of(2026, 12, 7));
            LocalDate newDate = LocalDate.of(2026, 12, 24);
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(classSlotRepository.existsByGroupClassIdAndClassDate(classId, newDate)).thenReturn(false);
            when(groupClassMapper.toDtoWithSlots(existing)).thenAnswer(invocation -> withSlotsDto(existing));

            // when
            GroupClassWithSlotsResponseDTO result = groupClassService.createClassSlot(
                    classId, new CreateClassSlotRequestDTO(newDate, "teams-x"));

            // then
            verify(classSlotRepository, never()).existsByGroupClassId(any());
            verify(classSlotRepository).save(slotCaptor.capture());
            ClassSlotEntity saved = slotCaptor.getValue();
            assertThat(saved.getClassDate()).isEqualTo(newDate);
            assertThat(saved.getMsTeamsCode()).isEqualTo("teams-x");
            assertThat(saved.getGroupClass()).isSameAs(existing);
            assertThat(existing.getSlots()).contains(saved).hasSize(2);
            assertThat(result.slots()).extracting(ClassSlotDTO::date).containsExactly("2026-12-07", "2026-12-24");
        }

        @Test
        void shouldAddSlotToOneTimeClassThatHasNoSlotYet() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, true);
            LocalDate newDate = LocalDate.of(2026, 12, 24);
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(classSlotRepository.existsByGroupClassId(classId)).thenReturn(false);
            when(classSlotRepository.existsByGroupClassIdAndClassDate(classId, newDate)).thenReturn(false);
            when(groupClassMapper.toDtoWithSlots(existing)).thenAnswer(invocation -> withSlotsDto(existing));

            // when
            groupClassService.createClassSlot(classId, new CreateClassSlotRequestDTO(newDate, null));

            // then
            verify(classSlotRepository).save(any(ClassSlotEntity.class));
            assertThat(existing.getSlots()).hasSize(1);
        }

        @Test
        void shouldRejectSecondSlotForOneTimeClass() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, true, LocalDate.of(2026, 12, 7));
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(classSlotRepository.existsByGroupClassId(classId)).thenReturn(true);

            // when and then
            assertThatThrownBy(() -> groupClassService.createClassSlot(
                    classId, new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 24), null)))
                    .isInstanceOf(ClassSlotConflictException.class)
                    .hasMessage(ExceptionErrorConstants.ONE_TIME_CLASS_ALREADY_HAS_SLOT);

            verify(classSlotRepository, never()).save(any());
            assertThat(existing.getSlots()).hasSize(1);
        }

        @Test
        void shouldRejectDuplicateDate() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false, LocalDate.of(2026, 12, 7));
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(classSlotRepository.existsByGroupClassIdAndClassDate(classId, LocalDate.of(2026, 12, 7)))
                    .thenReturn(true);

            // when and then
            assertThatThrownBy(() -> groupClassService.createClassSlot(
                    classId, new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 7), null)))
                    .isInstanceOf(ClassSlotConflictException.class)
                    .hasMessage(ExceptionErrorConstants.CLASS_SLOT_ALREADY_EXISTS);

            verify(classSlotRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenClassDoesNotExist() {
            // given
            UUID classId = UUID.randomUUID();
            when(groupClassRepository.findById(classId)).thenReturn(Optional.empty());

            // when and then
            assertThatThrownBy(() -> groupClassService.createClassSlot(
                    classId, new CreateClassSlotRequestDTO(LocalDate.of(2026, 12, 7), null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(ExceptionErrorConstants.GROUP_CLASS_NOT_FOUND);

            verifyNoInteractions(classSlotRepository);
        }
    }

    // ---------------------------------------------------------------- updateClassSlot

    @Nested
    class UpdateClassSlot {

        @Test
        void shouldMoveSlotToNewDateAndSetTeamsCode() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false,
                    LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 14));
            LocalDate newDate = LocalDate.of(2026, 12, 9);
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(classSlotRepository.existsByGroupClassIdAndClassDate(classId, newDate)).thenReturn(false);
            when(groupClassMapper.toDtoWithSlots(existing)).thenAnswer(invocation -> withSlotsDto(existing));

            // when
            GroupClassWithSlotsResponseDTO result = groupClassService.updateClassSlot(
                    classId, LocalDate.of(2026, 12, 7), new UpdateClassSlotRequestDTO(newDate, "teams-2"));

            // then
            assertThat(result.slots()).containsExactly(
                    new ClassSlotDTO("2026-12-09", "teams-2"),
                    new ClassSlotDTO("2026-12-14", null)
            );
        }

        @Test
        void shouldOnlyChangeTeamsCodeWithoutCheckingForConflicts() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false, LocalDate.of(2026, 12, 7));
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(groupClassMapper.toDtoWithSlots(existing)).thenAnswer(invocation -> withSlotsDto(existing));

            // when
            groupClassService.updateClassSlot(
                    classId, LocalDate.of(2026, 12, 7),
                    new UpdateClassSlotRequestDTO(LocalDate.of(2026, 12, 7), "teams-1"));

            // then
            verify(classSlotRepository, never()).existsByGroupClassIdAndClassDate(any(), any());
            assertThat(existing.getSlots().iterator().next().getMsTeamsCode()).isEqualTo("teams-1");
        }

        @Test
        void shouldClearTeamsCodeWhenNullIsSent() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false, LocalDate.of(2026, 12, 7));
            existing.getSlots().iterator().next().setMsTeamsCode("old");
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(groupClassMapper.toDtoWithSlots(existing)).thenAnswer(invocation -> withSlotsDto(existing));

            // when
            groupClassService.updateClassSlot(
                    classId, LocalDate.of(2026, 12, 7),
                    new UpdateClassSlotRequestDTO(LocalDate.of(2026, 12, 7), null));

            // then
            assertThat(existing.getSlots().iterator().next().getMsTeamsCode()).isNull();
        }

        @Test
        void shouldRejectMovingOntoAnExistingDateAndKeepTheSlotUnchanged() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false,
                    LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 14));
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(classSlotRepository.existsByGroupClassIdAndClassDate(classId, LocalDate.of(2026, 12, 14)))
                    .thenReturn(true);

            // when and then
            assertThatThrownBy(() -> groupClassService.updateClassSlot(
                    classId, LocalDate.of(2026, 12, 7),
                    new UpdateClassSlotRequestDTO(LocalDate.of(2026, 12, 14), "x")))
                    .isInstanceOf(ClassSlotConflictException.class)
                    .hasMessage(ExceptionErrorConstants.CLASS_SLOT_ALREADY_EXISTS);

            assertThat(slotDates(existing)).containsExactly(LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 14));
            assertThat(existing.getSlots()).allSatisfy(slot -> assertThat(slot.getMsTeamsCode()).isNull());
        }

        @Test
        void shouldThrowWhenSlotDoesNotExist() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false, LocalDate.of(2026, 12, 7));
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));

            // when and then
            assertThatThrownBy(() -> groupClassService.updateClassSlot(
                    classId, LocalDate.of(2027, 1, 1),
                    new UpdateClassSlotRequestDTO(LocalDate.of(2027, 1, 2), null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(ExceptionErrorConstants.CLASS_SLOT_NOT_FOUND);
        }

        @Test
        void shouldThrowWhenClassDoesNotExist() {
            // given
            UUID classId = UUID.randomUUID();
            when(groupClassRepository.findById(classId)).thenReturn(Optional.empty());

            // when and then
            assertThatThrownBy(() -> groupClassService.updateClassSlot(
                    classId, LocalDate.of(2026, 12, 7),
                    new UpdateClassSlotRequestDTO(LocalDate.of(2026, 12, 8), null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(ExceptionErrorConstants.GROUP_CLASS_NOT_FOUND);
        }
    }

    // ---------------------------------------------------------------- deleteClassSlot

    @Nested
    class DeleteClassSlot {

        @Test
        void shouldRemoveOnlyTheSlotOnTheGivenDate() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false,
                    LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 21));
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));
            when(groupClassMapper.toDtoWithSlots(existing)).thenAnswer(invocation -> withSlotsDto(existing));

            // when
            GroupClassWithSlotsResponseDTO result = groupClassService.deleteClassSlot(
                    classId, LocalDate.of(2026, 12, 14));

            // then
            assertThat(slotDates(existing)).containsExactly(LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 21));
            assertThat(result.slots()).extracting(ClassSlotDTO::date).containsExactly("2026-12-07", "2026-12-21");
        }

        @Test
        void shouldThrowWhenSlotDoesNotExist() {
            // given
            UUID classId = UUID.randomUUID();
            GroupClassEntity existing = existingClass(classId, false, LocalDate.of(2026, 12, 7));
            when(groupClassRepository.findById(classId)).thenReturn(Optional.of(existing));

            // when and then
            assertThatThrownBy(() -> groupClassService.deleteClassSlot(classId, LocalDate.of(2027, 1, 1)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(ExceptionErrorConstants.CLASS_SLOT_NOT_FOUND);

            assertThat(existing.getSlots()).hasSize(1);
        }

        @Test
        void shouldThrowWhenClassDoesNotExist() {
            // given
            UUID classId = UUID.randomUUID();
            when(groupClassRepository.findById(classId)).thenReturn(Optional.empty());

            // when and then
            assertThatThrownBy(() -> groupClassService.deleteClassSlot(classId, LocalDate.of(2026, 12, 7)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage(ExceptionErrorConstants.GROUP_CLASS_NOT_FOUND);
        }
    }

    // ---------------------------------------------------------------- helpers

    private void stubReferenceData() {
        when(subjectService.getById(subjectId)).thenReturn(subject);
        when(roomService.getById(roomId)).thenReturn(room);
        when(academicGroupService.getAllByIds(Set.of(groupId))).thenReturn(groups);
        when(userService.getLecturersByIds(Set.of(lecturerId))).thenReturn(lecturers);
    }

    private ScheduleGroupClassDTO scheduleEntry(DayOfWeek weekDay, LocalTime start, LocalTime end) {
        return new ScheduleGroupClassDTO(subjectId, roomId, Set.of(groupId), Set.of(lecturerId), weekDay, start, end);
    }

    private CreateGroupClassRequestDTO createRequest(boolean oneTime, List<CreateClassSlotRequestDTO> slots) {
        return new CreateGroupClassRequestDTO(
                subjectId, roomId, Set.of(groupId), Set.of(lecturerId), START, END, oneTime, slots);
    }

    private UpdateGroupClassRequestDTO updateRequest(LocalTime start, LocalTime end) {
        return new UpdateGroupClassRequestDTO(subjectId, roomId, Set.of(groupId), Set.of(lecturerId), start, end);
    }

    private GroupClassEntity existingClass(UUID id, boolean oneTime, LocalDate... dates) {
        GroupClassEntity groupClass = new GroupClassEntity();
        groupClass.setId(id);
        groupClass.setOneTime(oneTime);
        groupClass.setStartTime(START);
        groupClass.setEndTime(END);
        groupClass.setSubject(subject);
        groupClass.setRoom(room);
        groupClass.setGroups(new HashSet<>(groups));
        groupClass.setLecturers(new HashSet<>(lecturers));
        for (LocalDate date : dates) {
            ClassSlotEntity slot = new ClassSlotEntity();
            slot.setGroupClass(groupClass);
            slot.setClassDate(date);
            groupClass.getSlots().add(slot);
        }
        return groupClass;
    }

    private List<LocalDate> slotDates(GroupClassEntity groupClass) {
        return groupClass.getSlots().stream()
                .map(ClassSlotEntity::getClassDate)
                .sorted()
                .toList();
    }

    private GroupClassEntity findByStartTime(List<GroupClassEntity> classes, LocalTime startTime) {
        return classes.stream()
                .filter(groupClass -> groupClass.getStartTime().equals(startTime))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Stand-in for the real mapper: earliest slot date as classDate and sorted slots.
     */
    private GroupClassWithSlotsResponseDTO withSlotsDto(GroupClassEntity groupClass) {
        List<ClassSlotEntity> sortedSlots = groupClass.getSlots().stream()
                .sorted(Comparator.comparing(ClassSlotEntity::getClassDate))
                .toList();
        String classDate = sortedSlots.isEmpty() ? null : sortedSlots.get(0).getClassDate().toString();

        return new GroupClassWithSlotsResponseDTO(
                0, 0, "Subject", "SUB", "lecture", Set.of(), Set.of(), "Building", "Room",
                classDate,
                groupClass.getStartTime().toString(),
                groupClass.getEndTime().toString(),
                sortedSlots.stream()
                        .map(slot -> new ClassSlotDTO(slot.getClassDate().toString(), slot.getMsTeamsCode()))
                        .toList()
        );
    }

    private GroupClassResponseDTO dailyDto() {
        return new GroupClassResponseDTO(
                1, 1, "English B22", "ENG", "language course",
                Set.of("WIs I.1"), Set.of("Kowalski Jan"),
                "Main Campus", "Aula A", "2026-10-03", "08:15", "09:45", 90, "teams-123"
        );
    }
}
