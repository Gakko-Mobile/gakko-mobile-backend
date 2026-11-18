package com.gakkomobile.subject;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository subjectRepository;

    public SubjectEntity getById(UUID id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ExceptionErrorConstants.SUBJECT_NOT_FOUND));
    }
}
