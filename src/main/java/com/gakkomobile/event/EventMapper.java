package com.gakkomobile.event;

import com.gakkomobile.event.dto.EventResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(source = "building.name", target = "buildingName")
    @Mapping(source = "startTimestamp", target = "eventDate", qualifiedByName = "toDateString")
    @Mapping(source = "startTimestamp", target = "startTime", qualifiedByName = "toTimeString")
    @Mapping(source = "endTimestamp", target = "endTime", qualifiedByName = "toTimeString")
    EventResponseDTO toDto(EventEntity entity);

    @Named("toDateString")
    default String toDateString(OffsetDateTime dateTime) {
        if (Objects.isNull(dateTime)) {
            return null;
        }
        return dateTime.toLocalDate().toString();
    }

    @Named("toTimeString")
    default String toTimeString(OffsetDateTime dateTime) {
        if (Objects.isNull(dateTime)) {
            return null;
        }
        return dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}