package com.gakkomobile.studies;

import com.gakkomobile.studies.dto.StudiesResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudiesServiceTest {

    @Mock
    private StudiesRepository studiesRepository;

    @InjectMocks
    private StudiesService studiesService;

    @Test
    void getAll_ShouldReturnMappedResponses_WhenStudiesExist() {
        StudiesEntity study1 = new StudiesEntity();
        study1.setId(UUID.randomUUID());
        study1.setName("Computer Science - Web Development");

        StudiesEntity study2 = new StudiesEntity();
        study2.setId(UUID.randomUUID());
        study2.setName("New Media Arts - Graphic Design");

        when(studiesRepository.findAll()).thenReturn(List.of(study1, study2));

        List<StudiesResponse> result = studiesService.getAll();

        assertThat(result).hasSize(2);

        StudiesResponse response1 = result.get(0);
        assertThat(response1.id()).isEqualTo(study1.getId());
        assertThat(response1.name()).isEqualTo(study1.getName());

        StudiesResponse response2 = result.get(1);
        assertThat(response2.id()).isEqualTo(study2.getId());
        assertThat(response2.name()).isEqualTo(study2.getName());

        verify(studiesRepository).findAll();
    }

    @Test
    void getAll_ShouldReturnEmptyList_WhenNoStudiesExist() {
        when(studiesRepository.findAll()).thenReturn(Collections.emptyList());

        List<StudiesResponse> result = studiesService.getAll();

        assertThat(result).isEmpty();
        verify(studiesRepository).findAll();
    }
}