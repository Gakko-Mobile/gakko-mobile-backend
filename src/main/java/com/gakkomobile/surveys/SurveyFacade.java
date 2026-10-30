package com.gakkomobile.surveys;

import com.gakkomobile.surveys.dto.AdminSurveyDetailsDTO;
import com.gakkomobile.surveys.dto.CreateSurveyCommand;
import com.gakkomobile.surveys.dto.SubmitSurveyRequestDTO;
import com.gakkomobile.surveys.dto.SurveyAnalyticsDTO;
import com.gakkomobile.surveys.dto.SurveyResponseDTO;
import com.gakkomobile.surveys.dto.SurveyResultDTO;
import com.gakkomobile.surveys.dto.UpdateSurveyCommand;
import com.gakkomobile.surveys.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SurveyFacade {

    private final SurveyService surveyService;

    public AdminSurveyDetailsDTO createDraft(CreateSurveyCommand command, UUID creatorIdentifier) {

        return surveyService.createDraft(command, creatorIdentifier);
    }

    public AdminSurveyDetailsDTO updateSurvey(UUID surveyIdentifier, UpdateSurveyCommand command) {

        return surveyService.updateSurvey(surveyIdentifier, command);
    }

    public AdminSurveyDetailsDTO publishSurvey(UUID surveyIdentifier) {

        return surveyService.publishSurvey(surveyIdentifier);
    }

    public AdminSurveyDetailsDTO closeSurvey(UUID surveyIdentifier) {

        return surveyService.closeSurvey(surveyIdentifier);
    }

    public void softDeleteSurvey(UUID surveyIdentifier) {

        surveyService.softDeleteSurvey(surveyIdentifier);
    }

    public AdminSurveyDetailsDTO getAdminSurvey(UUID surveyIdentifier) {

        return surveyService.getAdminSurvey(surveyIdentifier);
    }

    public Page<AdminSurveyDetailsDTO> getAdminSurveys(Pageable pageable) {

        return surveyService.getAdminSurveys(pageable);
    }

    public SurveyAnalyticsDTO getAggregatedResults(UUID surveyIdentifier) {

        return surveyService.getAggregatedResults(surveyIdentifier);
    }

    public Page<SurveyResponseDTO> getPendingSurveys(UUID userIdentifier, Pageable pageable) {

        return surveyService.getPendingSurveys(userIdentifier, pageable);
    }

    public Page<SurveyResponseDTO> getCompletedSurveys(UUID userIdentifier, Pageable pageable) {

        return surveyService.getCompletedSurveys(userIdentifier, pageable);
    }

    public SurveyResponseDTO getSurveyForStudent(UUID surveyIdentifier, UUID userIdentifier) {

        return surveyService.getSurveyForStudent(surveyIdentifier, userIdentifier);
    }

    public void submitResponse(UUID surveyIdentifier, UUID userIdentifier, SubmitSurveyRequestDTO request) {

        surveyService.submitResponse(surveyIdentifier, userIdentifier, request);
    }

    public SurveyResultDTO getPublicResults(UUID surveyIdentifier, UUID userIdentifier) {

        return surveyService.getPublicResults(surveyIdentifier, userIdentifier);
    }
}
