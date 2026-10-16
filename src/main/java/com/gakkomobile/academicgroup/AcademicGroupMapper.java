package com.gakkomobile.academicgroup;

import com.gakkomobile.academicgroup.dto.AcademicGroupIdentifiersResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AcademicGroupMapper {

    @Mapping(source = "studies.name", target = "studiesName")
    AcademicGroupIdentifiersResponseDTO toDto(AcademicGroupEntity entity);
}