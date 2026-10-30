package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyQuestion;

import java.time.OffsetDateTime;
import java.util.List;

record SurveyConfiguration(
        String title,
        String description,
        List<SurveyQuestion> questions,
        AudienceTargetType targetType,
        List<String> targetIds,
        boolean anonymous,
        boolean allowResubmission,
        boolean requireLogin,
        boolean showResultsToStudents,
        boolean randomizeQuestionOrder,
        Integer responseLimit,
        String confirmationMessage,
        OffsetDateTime opensAt,
        OffsetDateTime closesAt,
        List<Double> reminderIntervals
) {
}
