package com.gakkomobile.groupclass;

import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/group-classes")
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
}
