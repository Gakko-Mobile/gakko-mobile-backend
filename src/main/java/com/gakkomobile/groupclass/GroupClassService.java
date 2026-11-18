package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupEntity;
import com.gakkomobile.academicgroup.AcademicGroupService;
import com.gakkomobile.academicgroup.exception.AcademicGroupNotFoundException;
import com.gakkomobile.exception.BadSearchParamsException;
import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.groupclass.dto.CreateScheduleRequestDTO;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.dto.ScheduleGroupClassDTO;
import com.gakkomobile.groupclass.exception.InvalidScheduleException;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
import com.gakkomobile.room.RoomEntity;
import com.gakkomobile.room.RoomService;
import com.gakkomobile.subject.SubjectEntity;
import com.gakkomobile.subject.SubjectService;
import com.gakkomobile.user.UserRepository;
import com.gakkomobile.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final UserRepository userRepository;
    private final AcademicGroupService academicGroupService;
    private final GroupClassMapper groupClassMapper;
    private final SubjectService subjectService;
    private final RoomService roomService;

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

                if (!userRepository.existsByEmail(userEmail)) {
                    throw new UserNotFoundException();
                }

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
    public List<GroupClassResponseDTO> createSchedule(CreateScheduleRequestDTO request) {
        validateSchedule(request);

        Map<UUID, SubjectEntity> subjects = new HashMap<>();
        Map<UUID, RoomEntity> rooms = new HashMap<>();
        Map<Set<UUID>, Set<AcademicGroupEntity>> groupsByIds = new HashMap<>();

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

            groupClasses.add(buildRecurringGroupClass(
                    groupClassDefinition,
                    request.startDate(),
                    request.endDate(),
                    subject,
                    room,
                    groups
            ));
        }

        return groupClassRepository.saveAll(groupClasses)
                .stream()
                .flatMap(groupClass -> groupClass.getSlots().stream())
                .sorted(Comparator
                        .comparing(ClassSlotEntity::getClassDate)
                        .thenComparing(slot -> slot.getGroupClass().getStartTime()))
                .map(groupClassMapper::toDto)
                .toList();
    }

    private GroupClassEntity buildRecurringGroupClass(
            ScheduleGroupClassDTO definition,
            LocalDate startDate,
            LocalDate endDate,
            SubjectEntity subject,
            RoomEntity room,
            Set<AcademicGroupEntity> groups
    ) {
        GroupClassEntity groupClass = new GroupClassEntity();
        groupClass.setSubject(subject);
        groupClass.setRoom(room);
        groupClass.setGroups(new HashSet<>(groups));
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