package com.gakkomobile.notifications.settings;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import com.gakkomobile.notifications.settings.dto.UserNotificationSettingsDTO;
import com.gakkomobile.notifications.settings.dto.UserNotificationSettingsResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = SPRING)
interface UserNotificationSettingsMapper {

    UserNotificationSettingsDTO entityToDTO(UserNotificationSettingsEntity entity);

    UserNotificationSettingsResponseDTO entityToResponseDTO(UserNotificationSettingsEntity entity);
}
