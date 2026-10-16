package com.gakkomobile.academicgroup;

import com.gakkomobile.academicgroup.dto.AcademicGroupIdentifiersResponseDTO;
import com.gakkomobile.studies.StudiesEntity;
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
class AcademicGroupServiceTest {

    @Mock
    private AcademicGroupRepository academicGroupRepository;

    @Mock
    private AcademicGroupMapper academicGroupMapper;

    @InjectMocks
    private AcademicGroupService academicGroupService;

    @Test
    void searchUserIdentifiers_ShouldReturnMappedResponses_WhenGroupsFound() {
        // given
        String studiesName = "Computer Science - Web Development";
        String academicYear = "2026/2027";

        StudiesEntity studies = new StudiesEntity();
        studies.setId(UUID.randomUUID());
        studies.setName(studiesName);

        AcademicGroupEntity group1 = new AcademicGroupEntity();
        group1.setId(UUID.randomUUID());
        group1.setName("WIs I.1");
        group1.setAcademicYear(academicYear);
        group1.setStudies(studies);

        AcademicGroupEntity group2 = new AcademicGroupEntity();
        group2.setId(UUID.randomUUID());
        group2.setName("WIs I.2");
        group2.setAcademicYear(academicYear);
        group2.setStudies(studies);

        AcademicGroupIdentifiersResponseDTO dto1 = new AcademicGroupIdentifiersResponseDTO(
                group1.getId(), studiesName, academicYear, group1.getName());
        AcademicGroupIdentifiersResponseDTO dto2 = new AcademicGroupIdentifiersResponseDTO(
                group2.getId(), studiesName, academicYear, group2.getName());

        when(academicGroupRepository.findAllByStudies_NameAndAcademicYear(studiesName, academicYear))
                .thenReturn(List.of(group1, group2));
        when(academicGroupMapper.toDto(group1)).thenReturn(dto1);
        when(academicGroupMapper.toDto(group2)).thenReturn(dto2);

        // when
        List<AcademicGroupIdentifiersResponseDTO> result = academicGroupService.searchUserIdentifiers(studiesName, academicYear);

        // then
        assertThat(result).hasSize(2);

        AcademicGroupIdentifiersResponseDTO response1 = result.get(0);
        assertThat(response1.id()).isEqualTo(group1.getId());
        assertThat(response1.studiesName()).isEqualTo(studiesName);
        assertThat(response1.academicYear()).isEqualTo(academicYear);
        assertThat(response1.name()).isEqualTo("WIs I.1");

        AcademicGroupIdentifiersResponseDTO response2 = result.get(1);
        assertThat(response2.id()).isEqualTo(group2.getId());
        assertThat(response2.name()).isEqualTo("WIs I.2");

        verify(academicGroupRepository).findAllByStudies_NameAndAcademicYear(studiesName, academicYear);
        verify(academicGroupMapper).toDto(group1);
        verify(academicGroupMapper).toDto(group2);
    }

    @Test
    void searchUserIdentifiers_ShouldReturnEmptyList_WhenNoGroupsFound() {
        // given
        String studiesName = "Unknown Studies";
        String academicYear = "2099/2100";

        when(academicGroupRepository.findAllByStudies_NameAndAcademicYear(studiesName, academicYear))
                .thenReturn(Collections.emptyList());

        // when
        List<AcademicGroupIdentifiersResponseDTO> result = academicGroupService.searchUserIdentifiers(studiesName, academicYear);

        // then
        assertThat(result).isEmpty();
        verify(academicGroupRepository).findAllByStudies_NameAndAcademicYear(studiesName, academicYear);
        verifyNoInteractions(academicGroupMapper);
    }
}