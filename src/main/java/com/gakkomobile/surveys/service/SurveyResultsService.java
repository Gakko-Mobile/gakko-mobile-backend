package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.dto.SurveyAnalyticsDTO;
import com.gakkomobile.surveys.dto.SurveyResultDTO;
import com.gakkomobile.surveys.exception.SurveyResultsHiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurveyResultsService {

    private final SurveyLookup surveyLookup;
    private final SurveyCompletionRepository surveyCompletionRepository;
    private final SurveyAnalyticsAggregator surveyAnalyticsAggregator;

    @Transactional(readOnly = true)
    public SurveyAnalyticsDTO getAggregatedResults(UUID surveyIdentifier) {

        return surveyAnalyticsAggregator.aggregateForStaff(surveyLookup.find(surveyIdentifier));
    }

    @Transactional(readOnly = true)
    public SurveyResultDTO getPublicResults(UUID surveyIdentifier, UUID userIdentifier) {

        SurveyEntity survey = surveyLookup.find(surveyIdentifier);
        if (!survey.isShowResultsToStudents()) {
            throw new SurveyResultsHiddenException("Survey results are hidden");
        }
        if (!surveyCompletionRepository.existsBySurveyIdAndUserId(surveyIdentifier, userIdentifier)) {
            throw new SurveyResultsHiddenException("Survey results are available after submission");
        }
        return surveyAnalyticsAggregator.aggregateForStudents(survey);
    }
}
