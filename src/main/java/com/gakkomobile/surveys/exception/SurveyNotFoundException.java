package com.gakkomobile.surveys.exception;

import com.gakkomobile.exception.ResourceNotFoundException;

import java.util.UUID;

public class SurveyNotFoundException extends ResourceNotFoundException {

    public SurveyNotFoundException(UUID surveyIdentifier) {

        super(String.format("Survey with id [%s] not found", surveyIdentifier));
    }
}
