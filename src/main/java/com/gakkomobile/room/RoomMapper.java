package com.gakkomobile.room;

import com.gakkomobile.room.dto.RoomIdentifiersResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(source = "building.name", target = "buildingName")
    RoomIdentifiersResponseDTO toDto(RoomEntity entity);
}
