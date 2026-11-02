package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyCompletionEntity;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyResponseEntity;
import com.gakkomobile.surveys.domain.SurveyStatus;
import com.gakkomobile.surveys.dto.SubmitSurveyRequestDTO;
import com.gakkomobile.surveys.exception.SurveyConflictException;
import com.gakkomobile.surveys.exception.SurveyNotFoundException;
import com.gakkomobile.surveys.exception.SurveyValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveySubmissionService {

    private final SurveyRepository surveyRepository;
    private final SurveyCompletionRepository surveyCompletionRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final DatabaseStudentAudienceLookup audienceLookup;
    private final SurveyAudienceMatcher surveyAudienceMatcher;
    private final SurveyAnswerValidator surveyAnswerValidator;

    @Transactional
    public void submitResponse(UUID surveyIdentifier, UUID userIdentifier, SubmitSurveyRequestDTO request) {

        SurveyEntity survey = surveyRepository.findByIdForUpdate(surveyIdentifier)
                .orElseThrow(() -> {
                    log.warn("Survey response rejected because survey {} was not found", surveyIdentifier);
                    return new SurveyNotFoundException(surveyIdentifier);
                });
        ensureAcceptingResponses(survey);
        if (!surveyAudienceMatcher.matches(
                survey,
                audienceLookup.findGroupTokens(userIdentifier),
                audienceLookup.findSubjectTokens(userIdentifier)
        )) {
            log.warn("Survey response rejected because the student is outside the audience: surveyId={}", surveyIdentifier);
            throw new SurveyNotFoundException(surveyIdentifier);
        }

        Optional<SurveyCompletionEntity> existingCompletion = surveyCompletionRepository
                .findBySurveyIdAndUserId(survey.getId(), userIdentifier);
        if (existingCompletion.isPresent() && !survey.isAllowResubmission()) {
            log.warn("Survey response rejected because it was already submitted: surveyId={}", surveyIdentifier);
            throw new SurveyConflictException("Survey response was already submitted");
        }

        Map<String, Object> answers = validatedAnswers(survey, request);
        OffsetDateTime currentTime = OffsetDateTime.now();
        saveCompletion(survey, userIdentifier, existingCompletion, currentTime);
        saveResponse(survey, userIdentifier, answers, currentTime);
        boolean closedByLimit = closeWhenResponseLimitReached(survey);
        log.info(
                "Survey response submitted: surveyId={}, anonymous={}, closedByLimit={}",
                surveyIdentifier,
                survey.isAnonymous(),
                closedByLimit
        );
    }

    private Map<String, Object> validatedAnswers(SurveyEntity survey, SubmitSurveyRequestDTO request) {

        try {
            Map<String, Object> answers = surveyAnswerValidator.toAnswerMap(request.answers());
            surveyAnswerValidator.validate(survey.getQuestions(), answers);
            return answers;
        } catch (SurveyValidationException exception) {
            log.warn("Survey response rejected: surveyId={}, reason={}", survey.getId(), exception.getMessage());
            throw exception;
        }
    }

    private void saveCompletion(SurveyEntity survey, UUID userIdentifier,
                                Optional<SurveyCompletionEntity> existingCompletion, OffsetDateTime currentTime) {

        if (existingCompletion.isPresent()) {
            existingCompletion.get().setCompletedAt(currentTime);
            return;
        }
        SurveyCompletionEntity completion = new SurveyCompletionEntity();
        completion.setSurveyId(survey.getId());
        completion.setUserId(userIdentifier);
        completion.setCompletedAt(currentTime);
        try {
            surveyCompletionRepository.saveAndFlush(completion);
        } catch (DataIntegrityViolationException exception) {
            if (surveyCompletionRepository.existsBySurveyIdAndUserId(survey.getId(), userIdentifier)) {
                log.warn("Survey response save completion rejected because it was already submitted: surveyId={}", survey.getId());
                throw new SurveyConflictException("Survey response was already submitted");
            }
            log.error("Survey completion could not be saved: surveyId={}", survey.getId(), exception);
            throw exception;
        }
    }

    private void saveResponse(SurveyEntity survey, UUID userIdentifier, Map<String, Object> answers,
                              OffsetDateTime currentTime) {

        if (survey.isAnonymous()) {
            SurveyResponseEntity response = new SurveyResponseEntity();
            response.setSurveyId(survey.getId());
            response.setUserId(null);
            response.setAnswers(answers);
            response.setSubmittedAt(currentTime);
            surveyResponseRepository.save(response);
            return;
        }

        Optional<SurveyResponseEntity> existingResponse = surveyResponseRepository
                .findBySurveyIdAndUserId(survey.getId(), userIdentifier);
        if (existingResponse.isPresent()) {
            SurveyResponseEntity response = existingResponse.get();
            response.setAnswers(answers);
            response.setSubmittedAt(currentTime);
            return;
        }

        SurveyResponseEntity response = new SurveyResponseEntity();
        response.setSurveyId(survey.getId());
        response.setUserId(userIdentifier);
        response.setAnswers(answers);
        response.setSubmittedAt(currentTime);
        surveyResponseRepository.save(response);
    }

    private boolean closeWhenResponseLimitReached(SurveyEntity survey) {

        if (Objects.isNull(survey.getResponseLimit())) {
            return false;
        }
        long responseCount = surveyResponseRepository.countBySurveyId(survey.getId());
        if (responseCount >= survey.getResponseLimit()) {
            survey.setStatus(SurveyStatus.CLOSED);
            return true;
        }
        return false;
    }

    private void ensureAcceptingResponses(SurveyEntity survey) {

        OffsetDateTime currentTime = OffsetDateTime.now();
        if (survey.getStatus() != SurveyStatus.COLLECTING) {
            log.warn("Survey response rejected because survey {} is {}", survey.getId(), survey.getStatus());
            throw new SurveyConflictException("Survey is not accepting responses");
        }
        if (!Objects.isNull(survey.getOpensAt()) && survey.getOpensAt().isAfter(currentTime)) {
            log.warn("Survey response rejected because survey {} is not open yet", survey.getId());
            throw new SurveyConflictException("Survey is not open yet");
        }
        if (!Objects.isNull(survey.getClosesAt()) && !survey.getClosesAt().isAfter(currentTime)) {
            log.warn("Survey response rejected because survey {} is closed", survey.getId());
            throw new SurveyConflictException("Survey is closed");
        }
    }
}
