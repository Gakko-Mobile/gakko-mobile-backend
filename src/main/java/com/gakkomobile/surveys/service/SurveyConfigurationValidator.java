package com.gakkomobile.surveys.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SurveyConfigurationValidator {

    private final SurveySettingsValidator surveySettingsValidator;
    private final SurveyQuestionDefinitionValidator surveyQuestionDefinitionValidator;

    void validate(SurveyConfiguration configuration) {

        surveySettingsValidator.validate(configuration);
        surveyQuestionDefinitionValidator.validate(configuration.questions());
    }
}
