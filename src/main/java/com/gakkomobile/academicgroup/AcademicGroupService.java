package com.gakkomobile.academicgroup;

import com.gakkomobile.academicgroup.dto.AcademicGroupIdentifiersResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicGroupService {
    private final AcademicGroupRepository academicGroupRepository;
    private final AcademicGroupMapper academicGroupMapper;

    public List<AcademicGroupIdentifiersResponseDTO> searchUserIdentifiers(
            String studiesName,
            String academicYear
    ) {
        return academicGroupRepository
                .findAllByStudies_NameAndAcademicYear(studiesName, academicYear)
                .stream()
                .map(academicGroupMapper::toDto)
                .toList();
    }

    public boolean existsById(UUID id) {
        return academicGroupRepository.existsById(id);
    }
}