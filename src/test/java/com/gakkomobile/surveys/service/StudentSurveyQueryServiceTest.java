package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyStatus;
import com.gakkomobile.surveys.dto.SurveyResponseDTO;
import com.gakkomobile.surveys.exception.SurveyNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentSurveyQueryServiceTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private SurveyCompletionRepository surveyCompletionRepository;
    @Mock
    private DatabaseStudentAudienceLookup audienceLookup;
    @Mock
    private SurveyAudienceMatcher surveyAudienceMatcher;
    @Mock
    private SurveyPresentation surveyPresentation;
    @Mock
    private SurveyLookup surveyLookup;

    @InjectMocks
    private StudentSurveyQueryService service;

    @Test
    void getPendingSurveys_shouldKeepOnlyOpenSurveysForTheStudentAudience() {

        UUID userIdentifier = UUID.randomUUID();
        SurveyEntity linkOnly = survey(AudienceTargetType.VIA_LINK);
        SurveyEntity completed = survey(AudienceTargetType.ALL);
        SurveyEntity hidden = survey(AudienceTargetType.GROUPS);
        SurveyEntity visible = survey(AudienceTargetType.ALL);
        SurveyResponseDTO visibleResponse = SurveyResponseDTO.builder().id(visible.getId()).title("Visible").build();
        when(surveyCompletionRepository.findSurveyIdentifiersByUserIdentifier(userIdentifier)).thenReturn(List.of(completed.getId()));
        when(audienceLookup.findGroupTokens(userIdentifier)).thenReturn(Set.of("CS-1"));
        when(audienceLookup.findSubjectTokens(userIdentifier)).thenReturn(Set.of());
        when(surveyRepository.findOpenSurveys(eq(SurveyStatus.COLLECTING), any())).thenReturn(List.of(linkOnly, completed, hidden, visible));
        when(surveyAudienceMatcher.matches(hidden, Set.of("CS-1"), Set.of())).thenReturn(false);
        when(surveyAudienceMatcher.matches(visible, Set.of("CS-1"), Set.of())).thenReturn(true);
        when(surveyPresentation.toStudentResponse(visible, userIdentifier, false, false)).thenReturn(visibleResponse);

        Page<SurveyResponseDTO> pending = service.getPendingSurveys(userIdentifier, PageRequest.of(0, 10));

        assertThat(pending.getTotalElements()).isEqualTo(1);
        assertThat(pending.getContent()).containsExactly(visibleResponse);
    }

    @Test
    void getCompletedSurveys_shouldReturnTheNewestCompletionFirst() {

        UUID userIdentifier = UUID.randomUUID();
        SurveyEntity older = survey(AudienceTargetType.ALL);
        older.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        SurveyEntity newer = survey(AudienceTargetType.ALL);
        newer.setCreatedAt(OffsetDateTime.parse("2026-02-01T00:00:00Z"));
        when(surveyCompletionRepository.findSurveyIdentifiersByUserIdentifier(userIdentifier))
                .thenReturn(List.of(older.getId(), newer.getId()));
        when(surveyRepository.findAllById(List.of(older.getId(), newer.getId()))).thenReturn(List.of(older, newer));
        when(surveyPresentation.toStudentResponse(eq(older), eq(userIdentifier), eq(true), eq(false)))
                .thenReturn(SurveyResponseDTO.builder().id(older.getId()).build());
        when(surveyPresentation.toStudentResponse(eq(newer), eq(userIdentifier), eq(true), eq(false)))
                .thenReturn(SurveyResponseDTO.builder().id(newer.getId()).build());

        Page<SurveyResponseDTO> completed = service.getCompletedSurveys(userIdentifier, PageRequest.of(0, 1));

        assertThat(completed.getTotalElements()).isEqualTo(2);
        assertThat(completed.getContent()).extracting(SurveyResponseDTO::id).containsExactly(newer.getId());
    }

    @Test
    void getSurveyForStudent_shouldHideSurveysOutsideTheAudienceUntilTheyAreCompleted() {

        UUID surveyIdentifier = UUID.randomUUID();
        UUID userIdentifier = UUID.randomUUID();
        SurveyEntity survey = survey(AudienceTargetType.GROUPS);
        survey.setId(surveyIdentifier);
        when(surveyLookup.find(surveyIdentifier)).thenReturn(survey);
        when(surveyCompletionRepository.existsBySurveyIdAndUserId(surveyIdentifier, userIdentifier)).thenReturn(false);
        when(audienceLookup.findGroupTokens(userIdentifier)).thenReturn(Set.of());
        when(audienceLookup.findSubjectTokens(userIdentifier)).thenReturn(Set.of());
        when(surveyAudienceMatcher.matches(survey, Set.of(), Set.of())).thenReturn(false);

        assertThatThrownBy(() -> service.getSurveyForStudent(surveyIdentifier, userIdentifier))
                .isInstanceOf(SurveyNotFoundException.class);

        when(surveyCompletionRepository.existsBySurveyIdAndUserId(surveyIdentifier, userIdentifier)).thenReturn(true);
        SurveyResponseDTO response = SurveyResponseDTO.builder().id(surveyIdentifier).build();
        when(surveyPresentation.toStudentResponse(survey, userIdentifier, true, true)).thenReturn(response);

        assertThat(service.getSurveyForStudent(surveyIdentifier, userIdentifier)).isEqualTo(response);
    }

    private SurveyEntity survey(AudienceTargetType targetType) {

        SurveyEntity survey = new SurveyEntity();
        survey.setId(UUID.randomUUID());
        survey.setTargetType(targetType);
        survey.setStatus(SurveyStatus.COLLECTING);
        return survey;
    }
}
