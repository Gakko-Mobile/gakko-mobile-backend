package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupEntity;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.user.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface GroupClassMapper {

    @Mapping(source = "groupClass.subject.name", target = "subjectName")
    @Mapping(source = "groupClass.subject.code", target = "subjectCode")
    @Mapping(source = "groupClass.subject.type", target = "classType", qualifiedByName = "formatClassType")
    @Mapping(source = "groupClass.room.building.name", target = "building")
    @Mapping(source = "groupClass.room.name", target = "room")
    @Mapping(source = "classDate", target = "classDate", qualifiedByName = "toDateString")
    @Mapping(source = "groupClass.startTime", target = "startTime", qualifiedByName = "toTimeString")
    @Mapping(source = "groupClass.endTime", target = "endTime", qualifiedByName = "toTimeString")
    @Mapping(source = "groupClass", target = "regularStudentsNum", qualifiedByName = "countRegularStudents")
    @Mapping(source = "groupClass", target = "itnStudentsNum", qualifiedByName = "countItnStudents")
    @Mapping(source = "groupClass.groups", target = "groups")
    @Mapping(source = "groupClass", target = "lecturers", qualifiedByName = "extractLecturers")
    @Mapping(source = "groupClass", target = "durationInMins", qualifiedByName = "calculateDuration")
    @Mapping(source = "groupClass.msTeamsCode", target = "msTeamsCode")
    GroupClassResponseDTO toDto(ClassSlotEntity slot);

    default String mapGroupToString(AcademicGroupEntity group) {
        return Objects.isNull(group) ? null : group.getName();
    }

    @Named("toDateString")
    default String toDateString(LocalDate date) {
        return Objects.isNull(date) ? null : date.toString();
    }

    @Named("toTimeString")
    default String toTimeString(LocalTime time) {
        return Objects.isNull(time) ? null : time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Named("formatClassType")
    default String formatClassType(Enum<?> type) {
        return Objects.isNull(type) ? null : type.name().toLowerCase().replace("_", " ");
    }

    @Named("calculateDuration")
    default int calculateDuration(GroupClassEntity entity) {
        if (Objects.isNull(entity)
                || Objects.isNull(entity.getStartTime())
                || Objects.isNull(entity.getEndTime())
        ) {
            return 0;
        }
        return (int) Duration.between(entity.getStartTime(), entity.getEndTime()).toMinutes();
    }

    @Named("countRegularStudents")
    default int countRegularStudents(GroupClassEntity entity) {
        if (Objects.isNull(entity) || Objects.isNull(entity.getGroups())) {
            return 0;
        }
        return (int) entity.getGroups().stream()
                .flatMap(g -> g.getMemberships().stream())
                .filter(m -> m.getUser().getRole() == Role.STUDENT && !m.isItn())
                .count();
    }

    @Named("countItnStudents")
    default int countItnStudents(GroupClassEntity entity) {
        if (Objects.isNull(entity) || Objects.isNull(entity.getGroups())) {
            return 0;
        }
        return (int) entity.getGroups().stream()
                .flatMap(g -> g.getMemberships().stream())
                .filter(m -> m.getUser().getRole() == Role.STUDENT && m.isItn())
                .count();
    }

    @Named("extractLecturers")
    default Set<String> extractLecturers(GroupClassEntity entity) {
        if (Objects.isNull(entity) || Objects.isNull(entity.getGroups())) {
            return new HashSet<>();
        }
        return entity.getGroups().stream()
                .flatMap(g -> g.getMemberships().stream())
                .filter(m -> m.getUser().getRole() == Role.LECTURER)
                .map(m -> m.getUser().getLastName() + " " + m.getUser().getFirstName())
                .collect(Collectors.toSet());
    }
}
