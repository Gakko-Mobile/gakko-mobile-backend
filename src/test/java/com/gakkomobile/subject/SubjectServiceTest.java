package com.gakkomobile.subject;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.subject.dto.SubjectIdentifiersResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SubjectMapper subjectMapper;

    @InjectMocks
    private SubjectService subjectService;

    @Test
    void getById_shouldReturnSubject_whenItExists() {
        // given
        UUID id = UUID.randomUUID();
        SubjectEntity subject = new SubjectEntity();
        when(subjectRepository.findById(id)).thenReturn(Optional.of(subject));

        // when and then
        assertThat(subjectService.getById(id)).isSameAs(subject);
    }

    @Test
    void getById_shouldThrowResourceNotFound_whenItDoesNotExist() {
        // given
        UUID id = UUID.randomUUID();
        when(subjectRepository.findById(id)).thenReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> subjectService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ExceptionErrorConstants.SUBJECT_NOT_FOUND);
    }

    @Test
    void searchSubjectIdentifiers_shouldQueryByNameOrCodeSortedByNameAndMapResults() {
        // given
        Pageable pageable = PageRequest.of(1, 5, Sort.by("name"));
        SubjectEntity subject = new SubjectEntity();
        SubjectIdentifiersResponseDTO dto = new SubjectIdentifiersResponseDTO(
                UUID.randomUUID(), "ENG", "English B22");

        when(subjectRepository.searchByNameOrCode("eng", pageable)).thenReturn(List.of(subject));
        when(subjectMapper.toDto(subject)).thenReturn(dto);

        // when
        List<SubjectIdentifiersResponseDTO> result = subjectService.searchSubjectIdentifiers("eng", 1, 5);

        // then
        assertThat(result).containsExactly(dto);
        verify(subjectRepository).searchByNameOrCode("eng", pageable);
    }

    @Test
    void searchSubjectIdentifiers_shouldReturnEmptyList_whenNothingMatches() {
        // given
        Pageable pageable = PageRequest.of(0, 5, Sort.by("name"));
        when(subjectRepository.searchByNameOrCode("zzz", pageable)).thenReturn(List.of());

        // when and then
        assertThat(subjectService.searchSubjectIdentifiers("zzz", 0, 5)).isEmpty();
        verifyNoInteractions(subjectMapper);
    }
}
