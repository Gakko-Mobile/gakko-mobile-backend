package com.gakkomobile.studies;

import com.gakkomobile.studies.dto.StudiesResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudiesMapper {

    StudiesResponseDTO toDto(StudiesEntity entity);
}