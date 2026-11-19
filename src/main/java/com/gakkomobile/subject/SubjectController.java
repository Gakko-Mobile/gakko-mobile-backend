package com.gakkomobile.subject;

import com.gakkomobile.subject.dto.SubjectIdentifiersResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @GetMapping("/identifiers")
    public ResponseEntity<List<SubjectIdentifiersResponseDTO>> searchSubjectIdentifiers(
            @RequestParam(name = "name-or-code-search") String nameOrCodeSearch,
            @RequestParam(name = "page", defaultValue = "0") Integer pageNum,
            @RequestParam(name = "limit", defaultValue = "5") Integer limit
    ) {
        List<SubjectIdentifiersResponseDTO> responseBody = subjectService.searchSubjectIdentifiers(
                nameOrCodeSearch,
                pageNum,
                limit
        );

        return ResponseEntity.ok(responseBody);
    }
}
