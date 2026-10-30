package com.gakkomobile.surveys;

import com.gakkomobile.surveys.dto.AdminSurveyDetailsDTO;
import com.gakkomobile.surveys.dto.CreateSurveyCommand;
import com.gakkomobile.surveys.dto.SurveyAnalyticsDTO;
import com.gakkomobile.surveys.dto.UpdateSurveyCommand;
import com.gakkomobile.surveys.service.SurveyService;
import com.gakkomobile.user.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/admin/surveys")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
public class AdminSurveyController {

    private final SurveyService surveyService;

    @PostMapping
    public ResponseEntity<AdminSurveyDetailsDTO> createSurvey(
            @AuthenticationPrincipal UserEntity currentUser,
            @Valid @RequestBody CreateSurveyCommand command
    ) {

        AdminSurveyDetailsDTO createdSurvey = surveyService.createDraft(command, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSurvey);
    }

    @GetMapping
    public ResponseEntity<Page<AdminSurveyDetailsDTO>> getSurveys(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        return ResponseEntity.ok(surveyService.getAdminSurveys(pageable));
    }

    @GetMapping("/{surveyId}")
    public ResponseEntity<AdminSurveyDetailsDTO> getSurvey(@PathVariable UUID surveyId) {

        return ResponseEntity.ok(surveyService.getAdminSurvey(surveyId));
    }

    @PutMapping("/{surveyId}")
    public ResponseEntity<AdminSurveyDetailsDTO> updateSurvey(
            @PathVariable UUID surveyId,
            @Valid @RequestBody UpdateSurveyCommand command
    ) {

        return ResponseEntity.ok(surveyService.updateSurvey(surveyId, command));
    }

    @PostMapping("/{surveyId}/publish")
    public ResponseEntity<AdminSurveyDetailsDTO> publishSurvey(@PathVariable UUID surveyId) {

        return ResponseEntity.ok(surveyService.publishSurvey(surveyId));
    }

    @PostMapping("/{surveyId}/close")
    public ResponseEntity<AdminSurveyDetailsDTO> closeSurvey(@PathVariable UUID surveyId) {

        return ResponseEntity.ok(surveyService.closeSurvey(surveyId));
    }

    @DeleteMapping("/{surveyId}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable UUID surveyId) {

        surveyService.softDeleteSurvey(surveyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{surveyId}/results")
    public ResponseEntity<SurveyAnalyticsDTO> getResults(@PathVariable UUID surveyId) {

        return ResponseEntity.ok(surveyService.getAggregatedResults(surveyId));
    }
}
