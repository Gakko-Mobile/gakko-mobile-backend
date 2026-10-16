package com.gakkomobile.user;

import com.gakkomobile.user.dto.UserIdentifiersResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "indexNumber", target = "index")
    @Mapping(source = ".", target = "fullName", qualifiedByName = "buildFullName")
    UserIdentifiersResponseDTO toDto(UserEntity entity);

    @Named("buildFullName")
    default String buildFullName(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return String.format("%s %s", entity.getLastName(), entity.getFirstName());
    }
}