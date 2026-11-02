package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SkipRule;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.dto.SkipLogicDTO;
import com.gakkomobile.surveys.dto.SurveyQuestionDTO;
import com.gakkomobile.surveys.dto.SurveyWriteCommand;
import com.github.f4b6a3.uuid.UuidCreator;
import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
interface SurveyMapper {

    default SurveyConfiguration toConfiguration(SurveyWriteCommand command) {

        return new SurveyConfiguration(
                command.title(),
                command.description(),
                mapQuestions(command.questions()),
                targetTypeOrAll(command.targetType()),
                SurveyCollections.copyStrings(command.targetIds()),
                defaultTrue(command.anonymous()),
                defaultFalse(command.allowResubmission()),
                defaultTrue(command.requireLogin()),
                defaultFalse(command.showResultsToStudents()),
                defaultFalse(command.randomizeQuestionOrder()),
                command.responseLimit(),
                command.confirmationMessage(),
                command.opensAt(),
                command.closesAt(),
                SurveyCollections.copyDoubles(command.reminderIntervals())
        );
    }

    @Mapping(target = "questions", expression = "java(SurveyCollections.copyQuestions(survey.getQuestions()))")
    @Mapping(target = "targetIds", expression = "java(SurveyCollections.copyStrings(survey.getTargetIds()))")
    @Mapping(target = "reminderIntervals", expression = "java(SurveyCollections.copyDoubles(survey.getReminderIntervals()))")
    @BeanMapping(ignoreUnmappedSourceProperties = {"id", "creator", "status", "deletedAt", "createdAt", "updatedAt"})
    SurveyConfiguration toConfiguration(SurveyEntity survey);

    @Mapping(target = "title", expression = "java(trimmedTitle(configuration.title()))")
    @Mapping(target = "questions", expression = "java(SurveyCollections.copyQuestions(configuration.questions()))")
    @Mapping(target = "targetIds", expression = "java(SurveyCollections.copyStrings(configuration.targetIds()))")
    @Mapping(target = "reminderIntervals", expression = "java(SurveyCollections.copyDoubles(configuration.reminderIntervals()))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void applyConfiguration(@MappingTarget SurveyEntity survey, SurveyConfiguration configuration);

    @Mapping(target = "id", expression = "java(questionId(question.id()))")
    @Mapping(target = "options", expression = "java(SurveyCollections.copyStrings(question.options()))")
    @Mapping(target = "skipRules", expression = "java(mapSkipRules(question.skipRules()))")
    SurveyQuestion mapQuestion(SurveyQuestionDTO question);

    @Mapping(target = "options", expression = "java(SurveyCollections.copyStrings(question.options()))")
    @Mapping(target = "skipRules", expression = "java(mapApiSkipRules(question.skipRules()))")
    SurveyQuestionDTO toApi(SurveyQuestion question);

    SkipRule mapSkipRule(SkipLogicDTO skipRule);

    SkipLogicDTO mapApiSkipRule(SkipRule skipRule);

    default List<SurveyQuestion> mapQuestions(List<SurveyQuestionDTO> questions) {

        List<SurveyQuestion> domainQuestions = new ArrayList<>();
        if (Objects.isNull(questions)) {
            return domainQuestions;
        }
        for (SurveyQuestionDTO question : questions) {
            domainQuestions.add(mapQuestion(question));
        }
        return domainQuestions;
    }

    default List<SurveyQuestionDTO> toApiQuestions(List<SurveyQuestion> questions) {

        List<SurveyQuestionDTO> mappedQuestions = new ArrayList<>();
        if (Objects.isNull(questions)) {
            return mappedQuestions;
        }
        for (SurveyQuestion question : questions) {
            mappedQuestions.add(toApi(question));
        }
        return mappedQuestions;
    }

    default List<SkipRule> mapSkipRules(List<SkipLogicDTO> skipRules) {

        List<SkipRule> domainRules = new ArrayList<>();
        if (Objects.isNull(skipRules)) {
            return domainRules;
        }
        for (SkipLogicDTO skipRule : skipRules) {
            domainRules.add(mapSkipRule(skipRule));
        }
        return domainRules;
    }

    default List<SkipLogicDTO> mapApiSkipRules(List<SkipRule> skipRules) {

        List<SkipLogicDTO> mappedSkipRules = new ArrayList<>();
        if (Objects.isNull(skipRules)) {
            return mappedSkipRules;
        }
        for (SkipRule skipRule : skipRules) {
            mappedSkipRules.add(mapApiSkipRule(skipRule));
        }
        return mappedSkipRules;
    }

    @Named("questionId")
    default String questionId(String id) {

        if (Objects.isNull(id) || id.isBlank()) {
            return UuidCreator.getTimeOrderedEpoch().toString();
        }
        return id;
    }

    @Named("defaultTrue")
    default boolean defaultTrue(Boolean value) {

        return Objects.isNull(value) || value;
    }

    @Named("defaultFalse")
    default boolean defaultFalse(Boolean value) {

        return !Objects.isNull(value) && value;
    }

    @Named("targetTypeOrAll")
    default AudienceTargetType targetTypeOrAll(AudienceTargetType targetType) {

        return Objects.isNull(targetType) ? AudienceTargetType.ALL : targetType;
    }

    @Named("trimmedTitle")
    default String trimmedTitle(String title) {

        return Objects.isNull(title) ? null : title.trim();
    }
}
