package com.gakkomobile.notifications.outbox;

import com.gakkomobile.avro.NotificationEventAvro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "processed", constant = "false")
    @Mapping(target = "payload", source = ".")
    NotificationOutboxEventEntity dtoToEntity(NotificationDTO dto);

    NotificationPayloadDTO toPayloadDTO(NotificationDTO dto);

    @Mapping(target = "eventId", source = "id")
    @Mapping(target = "title", source = "payload.title")
    @Mapping(target = "payload", source = "payload.body")
    @Mapping(target = "category", source = "payload.category")
    @Mapping(target = "targetScreen", source = "payload.targetScreen")
    NotificationEventAvro entityToAvro(NotificationOutboxEventEntity entity);
}
