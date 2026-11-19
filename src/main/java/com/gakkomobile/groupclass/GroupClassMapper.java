package com.gakkomobile.groupclass;

import com.gakkomobile.academicgroup.AcademicGroupEntity;
import com.gakkomobile.groupclass.dto.ClassSlotDTO;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.dto.GroupClassWithSlotsResponseDTO;
import com.gakkomobile.user.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
    GroupClassResponseDTO toDto(ClassSlotEntity slot);

    @Mapping(source = "subject.name", target = "subjectName")
    @Mapping(source = "subject.code", target = "subjectCode")
    @Mapping(source = "subject.type", target = "classType", qualifiedByName = "formatClassType")
    @Mapping(source = "room.building.name", target = "building")
    @Mapping(source = "room.name", target = "room")
    @Mapping(source = "slots", target = "classDate", qualifiedByName = "earliestSlotDate")
    @Mapping(source = "startTime", target = "startTime", qualifiedByName = "toTimeString")
    @Mapping(source = "endTime", target = "endTime", qualifiedByName = "toTimeString")
    @Mapping(source = ".", target = "regularStudentsNum", qualifiedByName = "countRegularStudents")
    @Mapping(source = ".", target = "itnStudentsNum", qualifiedByName = "countItnStudents")
    @Mapping(source = "groups", target = "groups")
    @Mapping(source = ".", target = "lecturers", qualifiedByName = "extractLecturers")
    @Mapping(source = "slots", target = "slots", qualifiedByName = "toSortedSlotDtos")
    GroupClassWithSlotsResponseDTO toDtoWithSlots(GroupClassEntity groupClass);

    @Named("earliestSlotDate")
    default String earliestSlotDate(Set<ClassSlotEntity> slots) {
        if (Objects.isNull(slots)) {
            return null;
        }
        return slots.stream()
                .map(ClassSlotEntity::getClassDate)
                .min(Comparator.naturalOrder())
                .map(LocalDate::toString)
                .orElse(null);
    }

    @Named("toSortedSlotDtos")
    default List<ClassSlotDTO> toSortedSlotDtos(Set<ClassSlotEntity> slots) {
        if (Objects.isNull(slots)) {
            return List.of();
        }
        return slots.stream()
                .sorted(Comparator.comparing(ClassSlotEntity::getClassDate))
                .map(slot -> new ClassSlotDTO(slot.getClassDate().toString(), slot.getMsTeamsCode()))
                .toList();
    }

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
        if (Objects.isNull(entity) || Objects.isNull(entity.getLecturers())) {
            return new HashSet<>();
        }
        return entity.getLecturers().stream()
                .map(lecturer -> lecturer.getLastName() + " " + lecturer.getFirstName())
                .collect(Collectors.toSet());
    }
}
