package com.gakkomobile.surveys.dto;

import com.gakkomobile.surveys.domain.AudienceTargetType;

import java.time.OffsetDateTime;
import java.util.List;

public interface SurveyWriteCommand {

    String title();

    String description();

    List<SurveyQuestionDTO> questions();

    AudienceTargetType targetType();

    List<String> targetIds();

    Boolean anonymous();

    Boolean allowResubmission();

    Boolean requireLogin();

    Boolean showResultsToStudents();

    Boolean randomizeQuestionOrder();

    Integer responseLimit();

    String confirmationMessage();

    OffsetDateTime opensAt();

    OffsetDateTime closesAt();

    List<Double> reminderIntervals();
}
