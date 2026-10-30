package com.gakkomobile.surveys;

import com.gakkomobile.surveys.dto.SubmitSurveyRequestDTO;
import com.gakkomobile.surveys.dto.SurveyResponseDTO;
import com.gakkomobile.surveys.dto.SurveyResultDTO;
import com.gakkomobile.surveys.service.SurveyService;
import com.gakkomobile.user.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/surveys")
@RequiredArgsConstructor
public class StudentSurveyController {

    private final SurveyService surveyService;

    @GetMapping("/pending")
    public ResponseEntity<Page<SurveyResponseDTO>> getPendingSurveys(
            @AuthenticationPrincipal UserEntity currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {

        return ResponseEntity.ok(surveyService.getPendingSurveys(currentUser.getId(), pageable));
    }

    @GetMapping("/completed")
    public ResponseEntity<Page<SurveyResponseDTO>> getCompletedSurveys(
            @AuthenticationPrincipal UserEntity currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {

        return ResponseEntity.ok(surveyService.getCompletedSurveys(currentUser.getId(), pageable));
    }

    @GetMapping("/{surveyId}")
    public ResponseEntity<SurveyResponseDTO> getSurvey(
            @AuthenticationPrincipal UserEntity currentUser,
            @PathVariable UUID surveyId
    ) {

        return ResponseEntity.ok(surveyService.getSurveyForStudent(surveyId, currentUser.getId()));
    }

    @PostMapping("/{surveyId}/responses")
    public ResponseEntity<Void> submitResponse(
            @AuthenticationPrincipal UserEntity currentUser,
            @PathVariable UUID surveyId,
            @Valid @RequestBody SubmitSurveyRequestDTO request
    ) {

        surveyService.submitResponse(surveyId, currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{surveyId}/public-results")
    public ResponseEntity<SurveyResultDTO> getPublicResults(
            @AuthenticationPrincipal UserEntity currentUser,
            @PathVariable UUID surveyId
    ) {

        return ResponseEntity.ok(surveyService.getPublicResults(surveyId, currentUser.getId()));
    }
}
