package com.gakkomobile.subject;

import com.gakkomobile.subject.dto.SubjectIdentifiersResponseDTO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubjectMapperTest {

    private final SubjectMapper mapper = Mappers.getMapper(SubjectMapper.class);

    @Test
    void toDto_shouldMapIdCodeAndName() {
        // given
        SubjectEntity subject = new SubjectEntity();
        subject.setId(UUID.randomUUID());
        subject.setCode("ENG");
        subject.setName("English B22");
        subject.setType(SubjectType.LANGUAGE_COURSE);
        subject.setEctsPoints(4);

        // when
        SubjectIdentifiersResponseDTO dto = mapper.toDto(subject);

        // then
        assertThat(dto).isEqualTo(new SubjectIdentifiersResponseDTO(subject.getId(), "ENG", "English B22"));
    }
}
