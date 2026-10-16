package com.gakkomobile.studies;

import com.gakkomobile.studies.dto.StudiesResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudiesService {
    private final StudiesRepository studiesRepository;

    public List<StudiesResponseDTO> getAll() {
        return studiesRepository
                .findAll()
                .stream()
                .map(s -> new StudiesResponseDTO(
                        s.getId(),
                        s.getName()
                )).toList();
    }
}
