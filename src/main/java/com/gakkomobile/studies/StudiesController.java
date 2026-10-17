package com.gakkomobile.studies;

import com.gakkomobile.studies.dto.StudiesResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/studies")
@RequiredArgsConstructor
public class StudiesController {
    private final StudiesService studiesService;

    @GetMapping
    public ResponseEntity<List<StudiesResponseDTO>> getAll() {
        return ResponseEntity.ok(studiesService.getAll());
    }
}
