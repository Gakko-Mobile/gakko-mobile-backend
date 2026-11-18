package com.gakkomobile.groupclass;

import com.gakkomobile.groupclass.dto.CreateScheduleRequestDTO;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/group-classes")
@RequiredArgsConstructor
public class GroupClassController {
    private final GroupClassService groupClassService;

    @GetMapping("/day")
    public ResponseEntity<List<GroupClassResponseDTO>> getDailySchedule(
            @RequestParam(name = "filter-option") ScheduleFilterOption filterOption,
            @RequestParam(name = "user-email", required = false) String userEmail,
            @RequestParam(name = "group-id", required = false) UUID groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<GroupClassResponseDTO> responseBody = groupClassService.getDailySchedule(
                filterOption,
                userEmail,
                groupId,
                date
        );

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<List<GroupClassResponseDTO>> createSchedule(
            @Valid @RequestBody CreateScheduleRequestDTO request
    ) {
        List<GroupClassResponseDTO> responseBody = groupClassService.createSchedule(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }
}
