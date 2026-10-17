package com.gakkomobile.academicgroup;

import com.gakkomobile.academicgroup.dto.AcademicGroupIdentifiersResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/academic-groups")
@RequiredArgsConstructor
public class AcademicGroupController {
    private final AcademicGroupService academicGroupService;

    @GetMapping("/identifiers")
    public ResponseEntity<List<AcademicGroupIdentifiersResponseDTO>> searchGroupIdentifiers(
            @RequestParam(name = "studies-name") String studiesName,
            @RequestParam(name = "academic-year") String academicYear
    ) {
        List<AcademicGroupIdentifiersResponseDTO> responseBody = academicGroupService.searchUserIdentifiers(
                studiesName,
                academicYear
        );

        return ResponseEntity.ok(responseBody);
    }
}
