package com.gakkomobile.surveys.service;

import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyStatus;
import com.gakkomobile.surveys.dto.AdminSurveyDetailsDTO;
import com.gakkomobile.surveys.dto.CreateSurveyCommand;
import com.gakkomobile.surveys.dto.UpdateSurveyCommand;
import com.gakkomobile.surveys.exception.SurveyConflictException;
import com.gakkomobile.surveys.exception.SurveyValidationException;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyAdministrationService {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final SurveyMapper surveyMapper;
    private final SurveyConfigurationValidator surveyConfigurationValidator;
    private final SurveyPresentation surveyPresentation;
    private final SurveyNotificationPublisher surveyNotificationPublisher;
    private final SurveyLookup surveyLookup;

    @Transactional
    public AdminSurveyDetailsDTO createDraft(CreateSurveyCommand command, UUID creatorIdentifier) {

        SurveyConfiguration configuration = surveyMapper.toConfiguration(command);
        validateConfiguration(null, configuration);
        UserEntity creator = userRepository.findById(creatorIdentifier)
                .orElseThrow(() -> {
                    log.warn("Survey draft rejected because creator {} was not found", creatorIdentifier);
                    return new ResourceNotFoundException(
                            String.format("User with id [%s] not found", creatorIdentifier));
                });

        SurveyEntity survey = new SurveyEntity();
        survey.setCreator(creator);
        survey.setStatus(SurveyStatus.DRAFT);
        surveyMapper.applyConfiguration(survey, configuration);
        SurveyEntity savedSurvey = surveyRepository.save(survey);
        log.info("Survey draft created: surveyId={}, creatorId={}", savedSurvey.getId(), creatorIdentifier);
        return surveyPresentation.toAdminDetails(savedSurvey);
    }

    @Transactional
    public AdminSurveyDetailsDTO updateSurvey(UUID surveyIdentifier, UpdateSurveyCommand command) {

        SurveyEntity survey = surveyLookup.find(surveyIdentifier);
        if (survey.getStatus() != SurveyStatus.DRAFT) {
            log.warn("Survey update rejected: surveyId={}, status={}", surveyIdentifier, survey.getStatus());
            throw new SurveyConflictException("Only draft surveys can be updated");
        }
        SurveyConfiguration configuration = surveyMapper.toConfiguration(command);
        validateConfiguration(surveyIdentifier, configuration);
        surveyMapper.applyConfiguration(survey, configuration);
        SurveyEntity savedSurvey = surveyRepository.save(survey);
        log.info("Survey draft updated: surveyId={}", savedSurvey.getId());
        return surveyPresentation.toAdminDetails(savedSurvey);
    }

    @Transactional
    public AdminSurveyDetailsDTO publishSurvey(UUID surveyIdentifier) {

        SurveyEntity survey = surveyLookup.find(surveyIdentifier);
        if (survey.getStatus() != SurveyStatus.DRAFT) {
            log.warn("Survey publish rejected: surveyId={}, status={}", surveyIdentifier, survey.getStatus());
            throw new SurveyConflictException("Only draft surveys can be published");
        }
        validateConfiguration(surveyIdentifier, surveyMapper.toConfiguration(survey));
        survey.setStatus(SurveyStatus.COLLECTING);
        SurveyEntity publishedSurvey = surveyRepository.save(survey);
        surveyNotificationPublisher.publishOpened(publishedSurvey);
        log.info("Survey published: surveyId={}", publishedSurvey.getId());
        return surveyPresentation.toAdminDetails(publishedSurvey);
    }

    @Transactional
    public AdminSurveyDetailsDTO closeSurvey(UUID surveyIdentifier) {

        SurveyEntity survey = surveyLookup.find(surveyIdentifier);
        if (survey.getStatus() != SurveyStatus.COLLECTING) {
            log.warn("Survey close rejected: surveyId={}, status={}", surveyIdentifier, survey.getStatus());
            throw new SurveyConflictException("Only collecting surveys can be closed");
        }
        survey.setStatus(SurveyStatus.CLOSED);
        SurveyEntity closedSurvey = surveyRepository.save(survey);
        log.info("Survey closed: surveyId={}", closedSurvey.getId());
        return surveyPresentation.toAdminDetails(closedSurvey);
    }

    @Transactional
    public void softDeleteSurvey(UUID surveyIdentifier) {

        SurveyEntity survey = surveyLookup.find(surveyIdentifier);
        survey.setDeletedAt(OffsetDateTime.now());
        surveyRepository.save(survey);
        log.info("Survey soft-deleted: surveyId={}", surveyIdentifier);
    }

    private void validateConfiguration(UUID surveyIdentifier, SurveyConfiguration configuration) {

        try {
            surveyConfigurationValidator.validate(configuration);
        } catch (SurveyValidationException exception) {
            log.warn("Survey configuration rejected: surveyId={}, reason={}", surveyIdentifier, exception.getMessage());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public AdminSurveyDetailsDTO getAdminSurvey(UUID surveyIdentifier) {

        return surveyPresentation.toAdminDetails(surveyLookup.find(surveyIdentifier));
    }

    @Transactional(readOnly = true)
    public Page<AdminSurveyDetailsDTO> getAdminSurveys(Pageable pageable) {

        return surveyRepository.findAll(pageable).map(surveyPresentation::toAdminDetails);
    }
}
