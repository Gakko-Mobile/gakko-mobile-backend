package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupRepository;
import com.gakkomobile.academicgroup.exception.AcademicGroupNotFoundException;
import com.gakkomobile.exception.BadSearchParamsException;
import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
import com.gakkomobile.user.UserRepository;
import com.gakkomobile.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupClassService {

    private final GroupClassRepository groupClassRepository;
    private final UserRepository userRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final GroupClassMapper groupClassMapper;

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
                checkIfSearchParamNullAndThrow(
                        userEmail,
                        ExceptionErrorConstants.EMAIL_NOT_PROVIDED
                );

                if (!userRepository.existsByEmail(userEmail)) {
                    throw new UserNotFoundException();
                }

                yield groupClassRepository
                        .findClassesByUserEmailAndDate(userEmail, startOfDay, endOfDay);
            }
            case GROUP -> {
                checkIfSearchParamNullAndThrow(
                        groupId,
                        ExceptionErrorConstants.ACADEMIC_GROUP_NAME_NOT_PROVIDED
                );

                if (!academicGroupRepository.existsById(groupId)) {
                    throw new AcademicGroupNotFoundException();
                }

                yield groupClassRepository
                        .findClassesByGroupIdAndDate(groupId, startOfDay, endOfDay);
            }
            default -> throw new RuntimeException("Not served ScheduleFilterOption value");
        };

        return groupClasses
                .stream()
                .map(groupClassMapper::toDto)
                .toList();
    }

    private void checkIfSearchParamNullAndThrow(Object param, String exceptionMessage) {
        if (Objects.isNull(param)) {
            throw new BadSearchParamsException(exceptionMessage);
        }
    }
}