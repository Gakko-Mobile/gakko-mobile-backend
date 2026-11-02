package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.exception.SurveyValidationException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
class SurveySettingsValidator {

    void validate(SurveyConfiguration configuration) {

        if (Objects.isNull(configuration.title()) || configuration.title().isBlank()) {
            throw new SurveyValidationException("Survey title is required");
        }
        if (configuration.anonymous() && configuration.allowResubmission()) {
            throw new SurveyValidationException("Anonymous surveys cannot allow resubmission");
        }
        if ((configuration.targetType() == AudienceTargetType.GROUPS || configuration.targetType() == AudienceTargetType.COURSE)
                && (Objects.isNull(configuration.targetIds()) || configuration.targetIds().isEmpty())) {
            throw new SurveyValidationException("Target identifiers are required for the selected audience");
        }
        if (!Objects.isNull(configuration.opensAt())
                && !Objects.isNull(configuration.closesAt())
                && !configuration.opensAt().isBefore(configuration.closesAt())) {
            throw new SurveyValidationException("Opening time must be before closing time");
        }
        if (!Objects.isNull(configuration.reminderIntervals()) && !configuration.reminderIntervals().isEmpty()
                && Objects.isNull(configuration.closesAt())) {
            throw new SurveyValidationException("Closing time is required when reminders are configured");
        }
        if (!Objects.isNull(configuration.reminderIntervals())) {
            for (Double reminderInterval : configuration.reminderIntervals()) {
                if (Objects.isNull(reminderInterval) || reminderInterval <= 0) {
                    throw new SurveyValidationException("Reminder intervals must be greater than zero");
                }
            }
        }
        if (!Objects.isNull(configuration.responseLimit()) && configuration.responseLimit() <= 0) {
            throw new SurveyValidationException("Response limit must be greater than zero");
        }
    }
}
