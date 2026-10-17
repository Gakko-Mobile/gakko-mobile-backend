package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupEntity;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.user.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface GroupClassMapper {

    @Mapping(source = "subject.name", target = "subjectName")
    @Mapping(source = "subject.code", target = "subjectCode")
    @Mapping(source = "subject.type", target = "classType", qualifiedByName = "formatClassType")
    @Mapping(source = "room.building.name", target = "building")
    @Mapping(source = "room.name", target = "room")
    @Mapping(source = "startTimestamp", target = "classDate", qualifiedByName = "toDateString")
    @Mapping(source = "startTimestamp", target = "startTime", qualifiedByName = "toTimeString")
    @Mapping(source = "endTimestamp", target = "endTime", qualifiedByName = "toTimeString")
    @Mapping(source = ".", target = "regularStudentsNum", qualifiedByName = "countRegularStudents")
    @Mapping(source = ".", target = "itnStudentsNum", qualifiedByName = "countItnStudents")
    @Mapping(source = "groups", target = "groups")
    @Mapping(source = ".", target = "lecturers", qualifiedByName = "extractLecturers")
    @Mapping(source = ".", target = "durationInMins", qualifiedByName = "calculateDuration")
    GroupClassResponseDTO toDto(GroupClassEntity entity);

    default String mapGroupToString(AcademicGroupEntity group) {
        return Objects.isNull(group) ? null : group.getName();
    }

    @Named("toDateString")
    default String toDateString(OffsetDateTime dateTime) {
        return Objects.isNull(dateTime) ? null : dateTime.toLocalDate().toString();
    }

    @Named("toTimeString")
    default String toTimeString(OffsetDateTime dateTime) {
        return Objects.isNull(dateTime) ? null : dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Named("formatClassType")
    default String formatClassType(Enum<?> type) {
        return Objects.isNull(type) ? null : type.name().toLowerCase().replace("_", " ");
    }

    @Named("calculateDuration")
    default int calculateDuration(GroupClassEntity entity) {
        if (Objects.isNull(entity)
                || Objects.isNull(entity.getStartTimestamp())
                || Objects.isNull(entity.getEndTimestamp())
        ) {
            return 0;
        }
        return (int) ChronoUnit.MINUTES.between(entity.getStartTimestamp(), entity.getEndTimestamp());
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