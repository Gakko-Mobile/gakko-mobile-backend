package com.gakkomobile.academicgroup;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AcademicGroupService {
    private final AcademicGroupRepository academicGroupRepository;
}
