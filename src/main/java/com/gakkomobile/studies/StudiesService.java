package com.gakkomobile.studies;

import com.gakkomobile.studies.dto.StudiesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudiesService {
    private final StudiesRepository studiesRepository;

    public List<StudiesResponse> getAll() {
        return studiesRepository
                .findAll()
                .stream()
                .map(s -> new StudiesResponse(
                        s.getId(),
                        s.getName()
                )).toList();
    }
}
