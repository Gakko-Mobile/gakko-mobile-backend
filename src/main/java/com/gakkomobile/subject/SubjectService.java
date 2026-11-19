package com.gakkomobile.subject;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.subject.dto.SubjectIdentifiersResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final SubjectMapper subjectMapper;

    public SubjectEntity getById(UUID id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ExceptionErrorConstants.SUBJECT_NOT_FOUND));
    }

    public List<SubjectIdentifiersResponseDTO> searchSubjectIdentifiers(
            String nameOrCodeSearch,
            int pageNum,
            int limit
    ) {
        Pageable pageable = PageRequest.of(pageNum, limit, Sort.by("name"));

        return subjectRepository.searchByNameOrCode(nameOrCodeSearch, pageable)
                .stream()
                .map(subjectMapper::toDto)
                .toList();
    }
}
