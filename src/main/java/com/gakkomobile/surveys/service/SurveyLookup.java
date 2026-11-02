package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.exception.SurveyNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class SurveyLookup {

    private final SurveyRepository surveyRepository;

    SurveyEntity find(UUID surveyIdentifier) {

        return surveyRepository.findById(surveyIdentifier)
                .orElseThrow(() -> new SurveyNotFoundException(surveyIdentifier));
    }
}
