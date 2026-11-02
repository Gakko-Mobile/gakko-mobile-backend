package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.dto.AdminSurveyDetailsDTO;
import com.gakkomobile.surveys.dto.SurveyQuestionDTO;
import com.gakkomobile.surveys.dto.SurveyResponseDTO;
import com.gakkomobile.user.UserEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
interface SurveyPresentationMapper {

    @Mapping(target = "id", source = "survey.id")
    @Mapping(target = "creatorId", source = "survey.creator.id")
    @Mapping(target = "authorName", expression = "java(authorName(survey.getCreator()))")
    @Mapping(target = "title", source = "survey.title")
    @Mapping(target = "description", source = "survey.description")
    @Mapping(target = "questions", source = "apiQuestions")
    @Mapping(target = "status", source = "survey.status")
    @Mapping(target = "targetType", source = "survey.targetType")
    @Mapping(target = "targetIds", expression = "java(immutableStrings(survey.getTargetIds()))")
    @Mapping(target = "anonymous", source = "survey.anonymous")
    @Mapping(target = "allowResubmission", source = "survey.allowResubmission")
    @Mapping(target = "requireLogin", source = "survey.requireLogin")
    @Mapping(target = "showResultsToStudents", source = "survey.showResultsToStudents")
    @Mapping(target = "randomizeQuestionOrder", source = "survey.randomizeQuestionOrder")
    @Mapping(target = "responseLimit", source = "survey.responseLimit")
    @Mapping(target = "confirmationMessage", source = "survey.confirmationMessage")
    @Mapping(target = "opensAt", source = "survey.opensAt")
    @Mapping(target = "closesAt", source = "survey.closesAt")
    @Mapping(target = "reminderIntervals", expression = "java(immutableDoubles(survey.getReminderIntervals()))")
    @Mapping(target = "createdAt", source = "survey.createdAt")
    @Mapping(target = "updatedAt", source = "survey.updatedAt")
    AdminSurveyDetailsDTO toAdminDetails(SurveyEntity survey, List<SurveyQuestionDTO> apiQuestions);

    @Mapping(target = "id", source = "survey.id")
    @Mapping(target = "title", source = "survey.title")
    @Mapping(target = "description", source = "survey.description")
    @Mapping(target = "authorName", expression = "java(authorName(survey.getCreator()))")
    @Mapping(target = "deadline", source = "survey.closesAt")
    @Mapping(target = "questionCount", expression = "java(questionCount(survey))")
    @Mapping(target = "privacyStatus", expression = "java(privacyStatus(survey.isAnonymous()))")
    @Mapping(target = "estimatedTimeLeft", expression = "java(estimatedTimeLeft(survey))")
    @Mapping(target = "isNew", expression = "java(isNew(survey))")
    @Mapping(target = "isCompleted", source = "completed")
    @Mapping(target = "status", source = "survey.status")
    @Mapping(target = "showResultsToStudents", source = "survey.showResultsToStudents")
    @Mapping(target = "allowResubmission", source = "survey.allowResubmission")
    @Mapping(target = "requireLogin", source = "survey.requireLogin")
    @Mapping(target = "confirmationMessage", source = "survey.confirmationMessage")
    @Mapping(target = "questions", source = "apiQuestions")
    SurveyResponseDTO toStudentResponse(SurveyEntity survey, boolean completed, List<SurveyQuestionDTO> apiQuestions);

    @Named("authorName")
    default String authorName(UserEntity creator) {

        if (Objects.isNull(creator)) {
            return "Unknown";
        }
        return creator.getFirstName() + " " + creator.getLastName();
    }

    @Named("questionCount")
    default int questionCount(SurveyEntity survey) {

        if (Objects.isNull(survey.getQuestions())) {
            return 0;
        }
        return survey.getQuestions().size();
    }

    @Named("privacyStatus")
    default String privacyStatus(boolean anonymous) {

        return anonymous ? "Anonymous" : "Public";
    }

    @Named("estimatedTimeLeft")
    default String estimatedTimeLeft(SurveyEntity survey) {

        int minutes = Math.max(questionCount(survey), 1);
        return "~" + minutes + " MIN LEFT";
    }

    @Named("isNew")
    default boolean isNew(SurveyEntity survey) {

        if (Objects.isNull(survey.getUpdatedAt())) {
            return false;
        }
        return survey.getUpdatedAt().isAfter(OffsetDateTime.now().minus(Duration.ofHours(48)));
    }

    @Named("immutableStrings")
    default List<String> immutableStrings(List<String> values) {

        return SurveyCollections.immutableStrings(values);
    }

    @Named("immutableDoubles")
    default List<Double> immutableDoubles(List<Double> values) {

        return SurveyCollections.immutableDoubles(values);
    }
}
