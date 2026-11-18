package com.gakkomobile.groupclass;

import com.gakkomobile.groupclass.dto.CreateClassSlotRequestDTO;
import com.gakkomobile.groupclass.dto.CreateGroupClassRequestDTO;
import com.gakkomobile.groupclass.dto.CreateScheduleRequestDTO;
import com.gakkomobile.groupclass.dto.GroupClassResponseDTO;
import com.gakkomobile.groupclass.dto.GroupClassWithSlotsResponseDTO;
import com.gakkomobile.groupclass.dto.UpdateClassSlotRequestDTO;
import com.gakkomobile.groupclass.dto.UpdateGroupClassRequestDTO;
import com.gakkomobile.groupclass.utils.ScheduleFilterOption;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<GroupClassWithSlotsResponseDTO> createGroupClass(
            @Valid @RequestBody CreateGroupClassRequestDTO request
    ) {
        GroupClassWithSlotsResponseDTO responseBody = groupClassService.createGroupClass(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<List<GroupClassWithSlotsResponseDTO>> createSchedule(
            @Valid @RequestBody CreateScheduleRequestDTO request
    ) {
        List<GroupClassWithSlotsResponseDTO> responseBody = groupClassService.createSchedule(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @PostMapping("/{id}/slots")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<GroupClassWithSlotsResponseDTO> createClassSlot(
            @PathVariable UUID id,
            @Valid @RequestBody CreateClassSlotRequestDTO request
    ) {
        GroupClassWithSlotsResponseDTO responseBody = groupClassService.createClassSlot(id, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @PutMapping("/{id}/slots/{date}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<GroupClassWithSlotsResponseDTO> updateClassSlot(
            @PathVariable UUID id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody UpdateClassSlotRequestDTO request
    ) {
        return ResponseEntity.ok(groupClassService.updateClassSlot(id, date, request));
    }

    @DeleteMapping("/{id}/slots/{date}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<GroupClassWithSlotsResponseDTO> deleteClassSlot(
            @PathVariable UUID id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(groupClassService.deleteClassSlot(id, date));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<GroupClassWithSlotsResponseDTO> updateGroupClass(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGroupClassRequestDTO request
    ) {
        return ResponseEntity.ok(groupClassService.updateGroupClass(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<Void> deleteGroupClass(@PathVariable UUID id) {
        groupClassService.deleteGroupClass(id);

        return ResponseEntity.noContent().build();
    }
}
