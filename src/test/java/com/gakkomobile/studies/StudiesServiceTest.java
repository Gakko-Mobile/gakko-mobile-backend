package com.gakkomobile.studies;

import com.gakkomobile.studies.dto.StudiesResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudiesServiceTest {

    @Mock
    private StudiesRepository studiesRepository;

    @Mock
    private StudiesMapper studiesMapper;

    @InjectMocks
    private StudiesService studiesService;

    @Test
    void getAll_ShouldReturnMappedResponses_WhenStudiesExist() {
        // given
        StudiesEntity study1 = new StudiesEntity();
        study1.setId(UUID.randomUUID());
        study1.setName("Computer Science - Web Development");

        StudiesEntity study2 = new StudiesEntity();
        study2.setId(UUID.randomUUID());
        study2.setName("New Media Arts - Graphic Design");

        StudiesResponseDTO dto1 = new StudiesResponseDTO(study1.getId(), study1.getName());
        StudiesResponseDTO dto2 = new StudiesResponseDTO(study2.getId(), study2.getName());

        when(studiesRepository.findAll()).thenReturn(List.of(study1, study2));
        when(studiesMapper.toDto(study1)).thenReturn(dto1);
        when(studiesMapper.toDto(study2)).thenReturn(dto2);

        // when
        List<StudiesResponseDTO> result = studiesService.getAll();

        // then
        assertThat(result).hasSize(2);

        StudiesResponseDTO response1 = result.get(0);
        assertThat(response1.id()).isEqualTo(study1.getId());
        assertThat(response1.name()).isEqualTo(study1.getName());

        StudiesResponseDTO response2 = result.get(1);
        assertThat(response2.id()).isEqualTo(study2.getId());
        assertThat(response2.name()).isEqualTo(study2.getName());

        verify(studiesRepository).findAll();
        verify(studiesMapper).toDto(study1);
        verify(studiesMapper).toDto(study2);
    }

    @Test
    void getAll_ShouldReturnEmptyList_WhenNoStudiesExist() {
        // given
        when(studiesRepository.findAll()).thenReturn(Collections.emptyList());

        // when
        List<StudiesResponseDTO> result = studiesService.getAll();

        // then
        assertThat(result).isEmpty();
        verify(studiesRepository).findAll();
        verifyNoInteractions(studiesMapper);
    }
}