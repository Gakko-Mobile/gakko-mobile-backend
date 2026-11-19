package com.gakkomobile.building;

import com.gakkomobile.building.dto.BuildingIdentifiersResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BuildingMapper {

    BuildingIdentifiersResponseDTO toDto(BuildingEntity entity);
}
