package com.gakkomobile.notifications.firebase;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import com.gakkomobile.notifications.firebase.dto.UserDeviceTokenRegisterRequestDTO;
import com.gakkomobile.notifications.firebase.dto.UserDeviceTokenResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
interface UserDeviceTokenMapper {

    @Mapping(target = "id", ignore = true)
    UserDeviceTokenEntity requestToEntity(UserDeviceTokenRegisterRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastUpdatedAt", source = "updatedAt")
    UserDeviceTokenResponseDTO entityToResponseDTO(UserDeviceTokenEntity entity);
}
