package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupEntity;
import com.gakkomobile.academicgroup.AcademicGroupService;
import com.gakkomobile.academicgroup.exception.AcademicGroupNotFoundException;
import com.gakkomobile.exception.BadSearchParamsException;
import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.groupclass.dto.CreateClassSlotRequestDTO;
import com.gakkomobile.groupclass.dto.CreateGroupClassRequestDTO;
import com.gakkomobile.groupclass.dto.CreateScheduleRequestDTO;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.dto.GroupClassWithSlotsResponseDTO;
import com.gakkomobile.groupclass.dto.UpdateClassSlotRequestDTO;
import com.gakkomobile.groupclass.dto.UpdateGroupClassRequestDTO;
import com.gakkomobile.groupclass.dto.ScheduleGroupClassDTO;
import com.gakkomobile.groupclass.exception.ClassSlotConflictException;
import com.gakkomobile.groupclass.exception.InvalidScheduleException;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
import com.gakkomobile.room.RoomEntity;
import com.gakkomobile.room.RoomService;
import com.gakkomobile.subject.SubjectEntity;
import com.gakkomobile.subject.SubjectService;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupClassService {

    private final GroupClassRepository groupClassRepository;
    private final ClassSlotRepository classSlotRepository;
    private final AcademicGroupService academicGroupService;
    private final GroupClassMapper groupClassMapper;
    private final SubjectService subjectService;
    private final RoomService roomService;
    private final UserService userService;

    static final int MAX_SCHEDULE_RANGE_IN_MONTHS = 5;

    public List<GroupClassResponseDTO> getDailySchedule(
            ScheduleFilterOption filterOption,
            String userEmail,
            UUID groupId,
            LocalDate date
    ) {
        List<ClassSlotEntity> slots = switch (filterOption) {
            case INDIVIDUAL -> {
                checkIfSearchParamNullAndThrow(
                        userEmail,
                        ExceptionErrorConstants.EMAIL_NOT_PROVIDED
                );

                userService.existsByEmail(userEmail);

                yield classSlotRepository.findSlotsByUserEmailAndDate(userEmail, date);
            }
            case GROUP -> {
                checkIfSearchParamNullAndThrow(
                        groupId,
                        ExceptionErrorConstants.ACADEMIC_GROUP_NAME_NOT_PROVIDED
                );

                if (!academicGroupService.existsById(groupId)) {
                    throw new AcademicGroupNotFoundException();
                }

                yield classSlotRepository.findSlotsByGroupIdAndDate(groupId, date);
            }
            default -> throw new RuntimeException("Not served ScheduleFilterOption value");
        };

        return slots
                .stream()
                .map(groupClassMapper::toDto)
                .toList();
    }

    @Transactional
    public List<GroupClassWithSlotsResponseDTO> createSchedule(CreateScheduleRequestDTO request) {
        validateSchedule(request);

        Map<UUID, SubjectEntity> subjects = new HashMap<>();
        Map<UUID, RoomEntity> rooms = new HashMap<>();
        Map<Set<UUID>, Set<AcademicGroupEntity>> groupsByIds = new HashMap<>();
        Map<Set<UUID>, Set<UserEntity>> lecturersByIds = new HashMap<>();

        List<GroupClassEntity> groupClasses = new ArrayList<>();
        for (ScheduleGroupClassDTO groupClassDefinition : request.groupClasses()) {
            SubjectEntity subject = subjects.computeIfAbsent(
                    groupClassDefinition.subjectId(),
                    subjectService::getById
            );
            RoomEntity room = rooms.computeIfAbsent(
                    groupClassDefinition.roomId(),
                    roomService::getById
            );
            Set<AcademicGroupEntity> groups = groupsByIds.computeIfAbsent(
                    groupClassDefinition.groupIds(),
                    academicGroupService::getAllByIds
            );
            Set<UserEntity> lecturers = lecturersByIds.computeIfAbsent(
                    groupClassDefinition.lecturerIds(),
                    userService::getLecturersByIds
            );

            groupClasses.add(buildRecurringGroupClass(
                    groupClassDefinition,
                    request.startDate(),
                    request.endDate(),
                    subject,
                    room,
                    groups,
                    lecturers
            ));
        }

        return groupClassRepository.saveAll(groupClasses)
                .stream()
                .map(groupClassMapper::toDtoWithSlots)
                .sorted(Comparator
                        .comparing(GroupClassWithSlotsResponseDTO::classDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(GroupClassWithSlotsResponseDTO::startTime))
                .toList();
    }

    @Transactional
    public GroupClassWithSlotsResponseDTO createGroupClass(CreateGroupClassRequestDTO request) {
        validateGroupClass(request);

        SubjectEntity subject = subjectService.getById(request.subjectId());
        RoomEntity room = roomService.getById(request.roomId());
        Set<AcademicGroupEntity> groups = academicGroupService.getAllByIds(request.groupIds());
        Set<UserEntity> lecturers = userService.getLecturersByIds(request.lecturerIds());

        GroupClassEntity groupClass = new GroupClassEntity();
        groupClass.setSubject(subject);
        groupClass.setRoom(room);
        groupClass.setGroups(new HashSet<>(groups));
        groupClass.setLecturers(new HashSet<>(lecturers));
        groupClass.setStartTime(request.startTime());
        groupClass.setEndTime(request.endTime());
        groupClass.setOneTime(request.oneTime());

        for (CreateClassSlotRequestDTO slotDefinition : request.slots()) {
            ClassSlotEntity slot = new ClassSlotEntity();
            slot.setGroupClass(groupClass);
            slot.setClassDate(slotDefinition.classDate());
            slot.setMsTeamsCode(slotDefinition.msTeamsCode());
            groupClass.getSlots().add(slot);
        }

        return groupClassMapper.toDtoWithSlots(groupClassRepository.save(groupClass));
    }

    @Transactional
    public GroupClassWithSlotsResponseDTO updateGroupClass(UUID id, UpdateGroupClassRequestDTO request) {
        GroupClassEntity groupClass = getGroupClassOrThrow(id);

        validateTimeRange(request.startTime(), request.endTime());

        groupClass.setSubject(subjectService.getById(request.subjectId()));
        groupClass.setRoom(roomService.getById(request.roomId()));
        groupClass.setGroups(new HashSet<>(academicGroupService.getAllByIds(request.groupIds())));
        groupClass.setLecturers(new HashSet<>(userService.getLecturersByIds(request.lecturerIds())));
        groupClass.setStartTime(request.startTime());
        groupClass.setEndTime(request.endTime());

        return groupClassMapper.toDtoWithSlots(groupClass);
    }

    @Transactional
    public void deleteGroupClass(UUID id) {
        GroupClassEntity groupClass = getGroupClassOrThrow(id);

        groupClassRepository.delete(groupClass);
    }

    @Transactional
    public GroupClassWithSlotsResponseDTO createClassSlot(
            UUID groupClassId,
            CreateClassSlotRequestDTO request
    ) {
        GroupClassEntity groupClass = getGroupClassOrThrow(groupClassId);

        if (groupClass.isOneTime() && classSlotRepository.existsByGroupClassId(groupClassId)) {
            throw new ClassSlotConflictException(ExceptionErrorConstants.ONE_TIME_CLASS_ALREADY_HAS_SLOT);
        }

        if (classSlotRepository.existsByGroupClassIdAndClassDate(groupClassId, request.classDate())) {
            throw new ClassSlotConflictException(ExceptionErrorConstants.CLASS_SLOT_ALREADY_EXISTS);
        }

        ClassSlotEntity slot = new ClassSlotEntity();
        slot.setGroupClass(groupClass);
        slot.setClassDate(request.classDate());
        slot.setMsTeamsCode(request.msTeamsCode());

        groupClass.getSlots().add(slot);
        classSlotRepository.save(slot);

        return groupClassMapper.toDtoWithSlots(groupClass);
    }

    @Transactional
    public GroupClassWithSlotsResponseDTO updateClassSlot(
            UUID groupClassId,
            LocalDate currentDate,
            UpdateClassSlotRequestDTO request
    ) {
        GroupClassEntity groupClass = getGroupClassOrThrow(groupClassId);
        ClassSlotEntity slot = getSlotOrThrow(groupClass, currentDate);

        boolean dateChanged = !request.classDate().equals(currentDate);
        if (dateChanged && classSlotRepository.existsByGroupClassIdAndClassDate(groupClassId, request.classDate())) {
            throw new ClassSlotConflictException(ExceptionErrorConstants.CLASS_SLOT_ALREADY_EXISTS);
        }

        slot.setClassDate(request.classDate());
        slot.setMsTeamsCode(request.msTeamsCode());

        return groupClassMapper.toDtoWithSlots(groupClass);
    }

    @Transactional
    public GroupClassWithSlotsResponseDTO deleteClassSlot(UUID groupClassId, LocalDate classDate) {
        GroupClassEntity groupClass = getGroupClassOrThrow(groupClassId);
        ClassSlotEntity slot = getSlotOrThrow(groupClass, classDate);

        groupClass.getSlots().remove(slot);

        return groupClassMapper.toDtoWithSlots(groupClass);
    }

    private GroupClassEntity getGroupClassOrThrow(UUID groupClassId) {
        return groupClassRepository.findById(groupClassId)
                .orElseThrow(() -> new ResourceNotFoundException(ExceptionErrorConstants.GROUP_CLASS_NOT_FOUND));
    }

    private ClassSlotEntity getSlotOrThrow(GroupClassEntity groupClass, LocalDate classDate) {
        return groupClass.getSlots().stream()
                .filter(existingSlot -> existingSlot.getClassDate().equals(classDate))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ExceptionErrorConstants.CLASS_SLOT_NOT_FOUND));
    }

    private GroupClassEntity buildRecurringGroupClass(
            ScheduleGroupClassDTO definition,
            LocalDate startDate,
            LocalDate endDate,
            SubjectEntity subject,
            RoomEntity room,
            Set<AcademicGroupEntity> groups,
            Set<UserEntity> lecturers
    ) {
        GroupClassEntity groupClass = new GroupClassEntity();
        groupClass.setSubject(subject);
        groupClass.setRoom(room);
        groupClass.setGroups(new HashSet<>(groups));
        groupClass.setLecturers(new HashSet<>(lecturers));
        groupClass.setStartTime(definition.startTime());
        groupClass.setEndTime(definition.endTime());
        groupClass.setOneTime(false);

        LocalDate date = startDate.with(TemporalAdjusters.nextOrSame(definition.weekDay()));
        while (!date.isAfter(endDate)) {
            ClassSlotEntity slot = new ClassSlotEntity();
            slot.setGroupClass(groupClass);
            slot.setClassDate(date);
            groupClass.getSlots().add(slot);

            date = date.plusWeeks(1);
        }

        return groupClass;
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidScheduleException(ExceptionErrorConstants.SCHEDULE_SLOT_END_TIME_NOT_AFTER_START_TIME);
        }
    }

    private void validateGroupClass(CreateGroupClassRequestDTO request) {
        validateTimeRange(request.startTime(), request.endTime());

        if (request.oneTime() && request.slots().size() > 1) {
            throw new InvalidScheduleException(ExceptionErrorConstants.ONE_TIME_CLASS_MULTIPLE_SLOTS);
        }

        long distinctDates = request.slots().stream()
                .map(CreateClassSlotRequestDTO::classDate)
                .distinct()
                .count();
        if (distinctDates != request.slots().size()) {
            throw new InvalidScheduleException(ExceptionErrorConstants.DUPLICATE_SLOT_DATES);
        }
    }

    private void validateSchedule(CreateScheduleRequestDTO request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidScheduleException(ExceptionErrorConstants.SCHEDULE_END_DATE_BEFORE_START_DATE);
        }

        if (request.endDate().isAfter(request.startDate().plusMonths(MAX_SCHEDULE_RANGE_IN_MONTHS))) {
            throw new InvalidScheduleException(
                    ExceptionErrorConstants.SCHEDULE_DATE_RANGE_TOO_LONG.formatted(MAX_SCHEDULE_RANGE_IN_MONTHS)
            );
        }

        boolean hasInvalidTimeRange = request.groupClasses().stream()
                .anyMatch(groupClass -> !groupClass.endTime().isAfter(groupClass.startTime()));
        if (hasInvalidTimeRange) {
            throw new InvalidScheduleException(ExceptionErrorConstants.SCHEDULE_SLOT_END_TIME_NOT_AFTER_START_TIME);
        }
    }

    private void checkIfSearchParamNullAndThrow(Object param, String exceptionMessage) {
        if (Objects.isNull(param)) {
            throw new BadSearchParamsException(exceptionMessage);
        }
    }
}