package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyCompletionEntity;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyResponseEntity;
import com.gakkomobile.surveys.domain.SurveyStatus;
import com.gakkomobile.surveys.dto.SubmitSurveyRequestDTO;
import com.gakkomobile.surveys.exception.SurveyConflictException;
import com.gakkomobile.surveys.exception.SurveyNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveySubmissionServiceTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private SurveyCompletionRepository surveyCompletionRepository;
    @Mock
    private SurveyResponseRepository surveyResponseRepository;
    @Mock
    private DatabaseStudentAudienceLookup audienceLookup;
    @Mock
    private SurveyAudienceMatcher surveyAudienceMatcher;
    @Mock
    private SurveyAnswerValidator surveyAnswerValidator;

    @InjectMocks
    private SurveySubmissionService service;

    @Test
    void submitResponse_shouldStoreAnAnonymousResponseWithoutAUserIdentifier() {

        UUID surveyIdentifier = UUID.randomUUID();
        UUID userIdentifier = UUID.randomUUID();
        SurveyEntity survey = collectingSurvey(surveyIdentifier, true);
        Map<String, Object> answers = Map.of("q1", "Too easy");
        when(surveyRepository.findByIdForUpdate(surveyIdentifier)).thenReturn(Optional.of(survey));
        when(audienceLookup.findGroupTokens(userIdentifier)).thenReturn(Set.of());
        when(audienceLookup.findSubjectTokens(userIdentifier)).thenReturn(Set.of());
        when(surveyAudienceMatcher.matches(survey, Set.of(), Set.of())).thenReturn(true);
        when(surveyCompletionRepository.findBySurveyIdAndUserId(surveyIdentifier, userIdentifier)).thenReturn(Optional.empty());
        when(surveyAnswerValidator.toAnswerMap(any())).thenReturn(answers);

        service.submitResponse(surveyIdentifier, userIdentifier, new SubmitSurveyRequestDTO(List.of()));

        ArgumentCaptor<SurveyResponseEntity> responseCaptor = ArgumentCaptor.forClass(SurveyResponseEntity.class);
        ArgumentCaptor<SurveyCompletionEntity> completionCaptor = ArgumentCaptor.forClass(SurveyCompletionEntity.class);
        verify(surveyResponseRepository).save(responseCaptor.capture());
        verify(surveyCompletionRepository).saveAndFlush(completionCaptor.capture());
        assertThat(responseCaptor.getValue().getUserId()).isNull();
        assertThat(responseCaptor.getValue().getAnswers()).isEqualTo(answers);
        assertThat(completionCaptor.getValue().getUserId()).isEqualTo(userIdentifier);
    }

    @Test
    void submitResponse_shouldRejectASecondSubmissionWhenResubmissionIsDisabled() {

        UUID surveyIdentifier = UUID.randomUUID();
        UUID userIdentifier = UUID.randomUUID();
        SurveyEntity survey = collectingSurvey(surveyIdentifier, true);
        when(surveyRepository.findByIdForUpdate(surveyIdentifier)).thenReturn(Optional.of(survey));
        when(audienceLookup.findGroupTokens(userIdentifier)).thenReturn(Set.of());
        when(audienceLookup.findSubjectTokens(userIdentifier)).thenReturn(Set.of());
        when(surveyAudienceMatcher.matches(survey, Set.of(), Set.of())).thenReturn(true);
        when(surveyCompletionRepository.findBySurveyIdAndUserId(surveyIdentifier, userIdentifier))
                .thenReturn(Optional.of(new SurveyCompletionEntity()));

        assertThatThrownBy(() -> service.submitResponse(surveyIdentifier, userIdentifier, new SubmitSurveyRequestDTO(List.of())))
                .isInstanceOf(SurveyConflictException.class)
                .hasMessage("Survey response was already submitted");
        verify(surveyResponseRepository, never()).save(any());
    }

    @Test
    void submitResponse_shouldCloseTheSurveyWhenTheResponseLimitIsReached() {

        UUID surveyIdentifier = UUID.randomUUID();
        UUID userIdentifier = UUID.randomUUID();
        SurveyEntity survey = collectingSurvey(surveyIdentifier, false);
        survey.setResponseLimit(1);
        when(surveyRepository.findByIdForUpdate(surveyIdentifier)).thenReturn(Optional.of(survey));
        when(audienceLookup.findGroupTokens(userIdentifier)).thenReturn(Set.of());
        when(audienceLookup.findSubjectTokens(userIdentifier)).thenReturn(Set.of());
        when(surveyAudienceMatcher.matches(survey, Set.of(), Set.of())).thenReturn(true);
        when(surveyCompletionRepository.findBySurveyIdAndUserId(surveyIdentifier, userIdentifier)).thenReturn(Optional.empty());
        when(surveyAnswerValidator.toAnswerMap(any())).thenReturn(Map.of());
        when(surveyResponseRepository.findBySurveyIdAndUserId(surveyIdentifier, userIdentifier)).thenReturn(Optional.empty());
        when(surveyResponseRepository.countBySurveyId(surveyIdentifier)).thenReturn(1L);

        service.submitResponse(surveyIdentifier, userIdentifier, new SubmitSurveyRequestDTO(List.of()));

        assertThat(survey.getStatus()).isEqualTo(SurveyStatus.CLOSED);
        ArgumentCaptor<SurveyResponseEntity> responseCaptor = ArgumentCaptor.forClass(SurveyResponseEntity.class);
        verify(surveyResponseRepository).save(responseCaptor.capture());
        assertThat(responseCaptor.getValue().getUserId()).isEqualTo(userIdentifier);
    }

    @Test
    void submitResponse_shouldTurnADuplicateCompletionIntoAConflict() {

        UUID surveyIdentifier = UUID.randomUUID();
        UUID userIdentifier = UUID.randomUUID();
        SurveyEntity survey = collectingSurvey(surveyIdentifier, true);
        when(surveyRepository.findByIdForUpdate(surveyIdentifier)).thenReturn(Optional.of(survey));
        when(audienceLookup.findGroupTokens(userIdentifier)).thenReturn(Set.of());
        when(audienceLookup.findSubjectTokens(userIdentifier)).thenReturn(Set.of());
        when(surveyAudienceMatcher.matches(survey, Set.of(), Set.of())).thenReturn(true);
        when(surveyCompletionRepository.findBySurveyIdAndUserId(surveyIdentifier, userIdentifier)).thenReturn(Optional.empty());
        when(surveyAnswerValidator.toAnswerMap(any())).thenReturn(Map.of());
        when(surveyCompletionRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(surveyCompletionRepository.existsBySurveyIdAndUserId(surveyIdentifier, userIdentifier)).thenReturn(true);

        assertThatThrownBy(() -> service.submitResponse(surveyIdentifier, userIdentifier, new SubmitSurveyRequestDTO(List.of())))
                .isInstanceOf(SurveyConflictException.class)
                .hasMessage("Survey response was already submitted");
    }

    @Test
    void submitResponse_shouldRejectAClosedSurveyAndAStudentOutsideTheAudience() {

        UUID surveyIdentifier = UUID.randomUUID();
        UUID userIdentifier = UUID.randomUUID();
        SurveyEntity closed = collectingSurvey(surveyIdentifier, true);
        closed.setStatus(SurveyStatus.CLOSED);
        when(surveyRepository.findByIdForUpdate(surveyIdentifier)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.submitResponse(surveyIdentifier, userIdentifier, new SubmitSurveyRequestDTO(List.of())))
                .isInstanceOf(SurveyConflictException.class)
                .hasMessage("Survey is not accepting responses");

        SurveyEntity future = collectingSurvey(surveyIdentifier, true);
        future.setOpensAt(OffsetDateTime.now().plusDays(1));
        when(surveyRepository.findByIdForUpdate(surveyIdentifier)).thenReturn(Optional.of(future));
        assertThatThrownBy(() -> service.submitResponse(surveyIdentifier, userIdentifier, new SubmitSurveyRequestDTO(List.of())))
                .isInstanceOf(SurveyConflictException.class)
                .hasMessage("Survey is not open yet");

        SurveyEntity hidden = collectingSurvey(surveyIdentifier, true);
        when(surveyRepository.findByIdForUpdate(surveyIdentifier)).thenReturn(Optional.of(hidden));
        when(audienceLookup.findGroupTokens(userIdentifier)).thenReturn(Set.of());
        when(audienceLookup.findSubjectTokens(userIdentifier)).thenReturn(Set.of());
        when(surveyAudienceMatcher.matches(hidden, Set.of(), Set.of())).thenReturn(false);
        assertThatThrownBy(() -> service.submitResponse(surveyIdentifier, userIdentifier, new SubmitSurveyRequestDTO(List.of())))
                .isInstanceOf(SurveyNotFoundException.class);
    }

    private SurveyEntity collectingSurvey(UUID surveyIdentifier, boolean anonymous) {

        SurveyEntity survey = new SurveyEntity();
        survey.setId(surveyIdentifier);
        survey.setStatus(SurveyStatus.COLLECTING);
        survey.setAnonymous(anonymous);
        survey.setTargetType(AudienceTargetType.ALL);
        survey.setQuestions(List.of());
        return survey;
    }
}
