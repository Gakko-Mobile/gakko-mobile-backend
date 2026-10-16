package com.gakkomobile.academicgroup;

import com.gakkomobile.academicgroup.dto.AcademicGroupIdentifiersResponse;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.User;
import com.gakkomobile.user.dto.UserIdentifiersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/academic-groups")
@RequiredArgsConstructor
public class AcademicGroupController {
    private final AcademicGroupService academicGroupService;

    @GetMapping("/identifiers")
    public ResponseEntity<List<AcademicGroupIdentifiersResponse>> searchGroupIdentifiers(
            @RequestParam(name = "studies-name") String studiesName,
            @RequestParam(name = "academic-year") String academicYear
    ) {
        List<AcademicGroupIdentifiersResponse> responseBody = academicGroupService.searchUserIdentifiers(
                studiesName,
                academicYear
        );

        return ResponseEntity.ok(responseBody);
    }
}
