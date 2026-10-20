package com.gakkomobile.notifications.push;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import com.gakkomobile.notifications.push.dto.UserDeviceTokenRegisterRequestDTO;
import com.gakkomobile.notifications.push.dto.UserDeviceTokenResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = SPRING)
interface UserDeviceTokenMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "token", source = "request.fcmToken")
    @Mapping(target = "userId", source = "userId")
    UserDeviceTokenEntity requestToEntity(UserDeviceTokenRegisterRequestDTO request, UUID userId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastUpdatedAt", source = "updatedAt")
    UserDeviceTokenResponseDTO entityToResponseDTO(UserDeviceTokenEntity entity);
}
