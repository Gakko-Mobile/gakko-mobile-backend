package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.dto.SurveyAnalyticsDTO;
import com.gakkomobile.surveys.dto.SurveyResultDTO;
import com.gakkomobile.surveys.exception.SurveyResultsHiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyResultsServiceTest {

    @Mock
    private SurveyLookup surveyLookup;
    @Mock
    private SurveyCompletionRepository surveyCompletionRepository;
    @Mock
    private SurveyAnalyticsAggregator surveyAnalyticsAggregator;

    @InjectMocks
    private SurveyResultsService service;

    @Test
    void getAggregatedResults_shouldAggregateTheRequestedSurvey() {

        UUID surveyIdentifier = UUID.randomUUID();
        SurveyEntity survey = new SurveyEntity();
        SurveyAnalyticsDTO analytics = SurveyAnalyticsDTO.builder().surveyId(surveyIdentifier).build();
        when(surveyLookup.find(surveyIdentifier)).thenReturn(survey);
        when(surveyAnalyticsAggregator.aggregateForStaff(survey)).thenReturn(analytics);

        assertThat(service.getAggregatedResults(surveyIdentifier)).isEqualTo(analytics);
    }

    @Test
    void getPublicResults_shouldStayHiddenUntilTheStudentHasSubmitted() {

        UUID surveyIdentifier = UUID.randomUUID();
        UUID userIdentifier = UUID.randomUUID();
        SurveyEntity hidden = new SurveyEntity();
        hidden.setShowResultsToStudents(false);
        when(surveyLookup.find(surveyIdentifier)).thenReturn(hidden);

        assertThatThrownBy(() -> service.getPublicResults(surveyIdentifier, userIdentifier))
                .isInstanceOf(SurveyResultsHiddenException.class)
                .hasMessage("Survey results are hidden");

        SurveyEntity visible = new SurveyEntity();
        visible.setShowResultsToStudents(true);
        when(surveyLookup.find(surveyIdentifier)).thenReturn(visible);
        when(surveyCompletionRepository.existsBySurveyIdAndUserId(surveyIdentifier, userIdentifier)).thenReturn(false);
        assertThatThrownBy(() -> service.getPublicResults(surveyIdentifier, userIdentifier))
                .isInstanceOf(SurveyResultsHiddenException.class)
                .hasMessage("Survey results are available after submission");

        when(surveyCompletionRepository.existsBySurveyIdAndUserId(surveyIdentifier, userIdentifier)).thenReturn(true);
        SurveyResultDTO result = SurveyResultDTO.builder().surveyId(surveyIdentifier).build();
        when(surveyAnalyticsAggregator.aggregateForStudents(visible)).thenReturn(result);

        assertThat(service.getPublicResults(surveyIdentifier, userIdentifier)).isEqualTo(result);
    }
}
