package com.gakkomobile.subject;

import com.gakkomobile.subject.dto.SubjectIdentifiersResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubjectMapper {

    SubjectIdentifiersResponseDTO toDto(SubjectEntity entity);
}
