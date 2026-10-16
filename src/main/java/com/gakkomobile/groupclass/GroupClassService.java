package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupEntity;
import com.gakkomobile.academicgroup.AcademicGroupRepository;
import com.gakkomobile.academicgroup.exception.AcademicGroupNotFoundException;
import com.gakkomobile.exception.BadSearchParamsException;
import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
import com.gakkomobile.user.exception.UserNotFoundException;
import com.gakkomobile.groupmembership.GroupMembershipEntity;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.room.RoomEntity;
import com.gakkomobile.subject.SubjectEntity;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupClassService {
    private final GroupClassRepository groupClassRepository;
    private final UserRepository userRepository;
    private final AcademicGroupRepository academicGroupRepository;

    public List<GroupClassResponseDTO> getDailySchedule(
            ScheduleFilterOption filterOption,
            String userEmail,
            UUID groupId,
            LocalDate date
    ) {
        OffsetDateTime startOfDay = date.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        List<GroupClassEntity> groupClasses = switch (filterOption) {
            case INDIVIDUAL -> {
                if (Objects.isNull(userEmail)) {
                    throw new BadSearchParamsException(
                            ExceptionErrorConstants.EMAIL_NOT_PROVIDED
                    );
                }

                if (!userRepository.existsByEmail(userEmail)) {
                    throw new UserNotFoundException();
                }

                yield groupClassRepository
                        .findClassesByUserEmailAndDate(userEmail, startOfDay, endOfDay);
            }
            case GROUP -> {
                if (Objects.isNull(groupId)) {
                    throw new BadSearchParamsException(
                            ExceptionErrorConstants.ACADEMIC_GROUP_NAME_NOT_PROVIDED
                    );
                }

                if (!academicGroupRepository.existsById(groupId)) {
                    throw new AcademicGroupNotFoundException();
                }
                yield groupClassRepository
                        .findClassesByGroupIdAndDate(groupId, startOfDay, endOfDay);
            }

            default -> throw new RuntimeException("Not served ScheduleFilterOption value");
        };

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return groupClasses
                .stream()
                .map(gc -> convertGroupClassToResponse(gc, timeFormatter))
                .toList();
    }

    private GroupClassResponseDTO convertGroupClassToResponse(
            GroupClassEntity groupClass,
            DateTimeFormatter timeFormatter
    ) {
        int regularStudentsNum = 0;
        int itnStudentsNum = 0;
        Set<String> lecturerNames = new HashSet<>();
        Set<String> groupNames = new HashSet<>();

        for (AcademicGroupEntity group : groupClass.getGroups()) {
            groupNames.add(group.getName());

            for (GroupMembershipEntity membership : group.getMemberships()) {
                UserEntity user = membership.getUser();

                if (user.getRole() == Role.STUDENT) {
                    if (membership.isItn()) {
                        itnStudentsNum++;
                    } else {
                        regularStudentsNum++;
                    }
                } else if (user.getRole() == Role.LECTURER) {
                    lecturerNames.add(user.getLastName() + " " + user.getFirstName());
                }
            }
        }

        SubjectEntity subject = groupClass.getSubject();

        String subjectTypeStr = subject
                .getType()
                .toString()
                .toLowerCase()
                .replace("_", " ");

        RoomEntity room = groupClass.getRoom();

        OffsetDateTime classStartTimestamp = groupClass.getStartTimestamp();
        OffsetDateTime classEndTimestamp = groupClass.getEndTimestamp();

        return new GroupClassResponseDTO(
                regularStudentsNum,
                itnStudentsNum,
                subject.getName(),
                subject.getCode(),
                subjectTypeStr,
                groupNames,
                lecturerNames,
                room.getBuilding()
                        .getName(),
                room.getName(),
                classStartTimestamp
                        .toLocalDate()
                        .toString(),
                classStartTimestamp
                        .toLocalTime()
                        .format(timeFormatter),
                classEndTimestamp
                        .toLocalTime()
                        .format(timeFormatter),
                (int) ChronoUnit
                        .MINUTES
                        .between(classStartTimestamp, classEndTimestamp),
                groupClass.getMsTeamsCode()
        );
    }
}
