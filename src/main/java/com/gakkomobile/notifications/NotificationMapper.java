package com.gakkomobile.notifications;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import com.gakkomobile.avro.NotificationEventAvro;
import com.gakkomobile.notifications.dto.InAppNotificationDTO;
import com.gakkomobile.notifications.dto.InAppNotificationResponseDTO;
import com.gakkomobile.notifications.dto.NotificationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
interface NotificationMapper {

    NotificationDTO avroToDTO(NotificationEventAvro event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "targetId")
    InAppNotificationEntity dtoToEntity(NotificationDTO dto);

    InAppNotificationResponseDTO entityToResponseDTO(InAppNotificationEntity entity);

    InAppNotificationDTO entityToDTO(InAppNotificationEntity entity);
}
